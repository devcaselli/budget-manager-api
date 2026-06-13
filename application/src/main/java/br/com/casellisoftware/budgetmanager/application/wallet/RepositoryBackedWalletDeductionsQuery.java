package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class RepositoryBackedWalletDeductionsQuery implements WalletDeductionsQuery {

    private final SubscriptionRepository subscriptionRepository;
    private final InstallmentRepository installmentRepository;
    private final ShareRepository shareRepository;
    private final ReservedBudgetRepository reservedBudgetRepository;

    public RepositoryBackedWalletDeductionsQuery(SubscriptionRepository subscriptionRepository,
                                                 InstallmentRepository installmentRepository,
                                                 ShareRepository shareRepository,
                                                 ReservedBudgetRepository reservedBudgetRepository) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository, "reservedBudgetRepository must not be null");
    }

    @Override
    public WalletDeductions forWallet(Wallet wallet) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        List<Subscription> activeSubscriptions = WalletSubscriptionSelector.activeForWallet(subscriptionRepository, wallet);
        List<Installment> activeInstallments = installmentRepository.findActiveAffecting(wallet.getEffectiveMonth(), wallet.getOwnerId());
        List<ReservedBudget> activeReservedBudgets = reservedBudgetRepository.findActiveFor(wallet.getEffectiveMonth(), wallet.getOwnerId());
        return toDeductions(wallet, activeSubscriptions, activeInstallments, activeReservedBudgets);
    }

    @Override
    public Map<String, WalletDeductions> forWallets(List<Wallet> wallets) {
        Objects.requireNonNull(wallets, "wallets must not be null");
        if (wallets.isEmpty()) {
            return Map.of();
        }

        Map<SubscriptionCacheKey, List<Subscription>> subscriptionCache = buildSubscriptionCache(wallets);
        Map<OwnerMonthKey, List<Installment>> installmentCache = buildInstallmentCache(wallets);
        Map<OwnerMonthKey, List<ReservedBudget>> reservedBudgetCache = buildReservedBudgetCache(wallets);

        return wallets.stream().collect(Collectors.toMap(
                Wallet::getId,
                wallet -> toDeductions(
                        wallet,
                        subscriptionCache.getOrDefault(cacheKeyFor(wallet), List.of()),
                        installmentCache.getOrDefault(new OwnerMonthKey(wallet.getOwnerId(), wallet.getEffectiveMonth()), List.of()),
                        reservedBudgetCache.getOrDefault(new OwnerMonthKey(wallet.getOwnerId(), wallet.getEffectiveMonth()), List.of())
                )
        ));
    }

    private Map<SubscriptionCacheKey, List<Subscription>> buildSubscriptionCache(List<Wallet> wallets) {
        return wallets.stream()
                .map(this::cacheKeyFor)
                .distinct()
                .collect(Collectors.toMap(
                        key -> key,
                        key -> subscriptionRepository.findActiveFor(key.month(), key.state(), key.ownerId())
                ));
    }

    private Map<OwnerMonthKey, List<Installment>> buildInstallmentCache(List<Wallet> wallets) {
        Collection<OwnerMonthKey> keys = wallets.stream()
                .map(wallet -> new OwnerMonthKey(wallet.getOwnerId(), wallet.getEffectiveMonth()))
                .distinct()
                .toList();
        Map<String, List<YearMonth>> monthsByOwner = keys.stream()
                .collect(Collectors.groupingBy(
                        OwnerMonthKey::ownerId,
                        Collectors.mapping(OwnerMonthKey::month, Collectors.toList())
                ));
        return keys.stream().collect(Collectors.toMap(
                key -> key,
                key -> installmentRepository.findActiveAffectingAny(monthsByOwner.get(key.ownerId()), key.ownerId()).stream()
                        .filter(installment -> InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, key.month()))
                        .toList()
        ));
    }

    private Map<OwnerMonthKey, List<ReservedBudget>> buildReservedBudgetCache(List<Wallet> wallets) {
        Collection<OwnerMonthKey> keys = wallets.stream()
                .map(wallet -> new OwnerMonthKey(wallet.getOwnerId(), wallet.getEffectiveMonth()))
                .distinct()
                .toList();
        Map<String, List<YearMonth>> monthsByOwner = keys.stream()
                .collect(Collectors.groupingBy(
                        OwnerMonthKey::ownerId,
                        Collectors.mapping(OwnerMonthKey::month, Collectors.toList())
                ));
        return keys.stream().collect(Collectors.toMap(
                key -> key,
                key -> reservedBudgetRepository.findActiveForAny(monthsByOwner.get(key.ownerId()), key.ownerId()).stream()
                        .filter(reservedBudget -> reservedBudget.isApplicable(key.month()))
                        .toList()
        ));
    }

    private WalletDeductions toDeductions(Wallet wallet,
                                          List<Subscription> activeSubscriptions,
                                          List<Installment> activeInstallments,
                                          List<ReservedBudget> activeReservedBudgets) {
        Money subscriptions = SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, activeSubscriptions, shareRepository);
        Money installments = InstallmentWalletBalanceCalculator.installmentTotal(wallet, activeInstallments, shareRepository);
        Money reservedBudgets = ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(wallet, activeReservedBudgets);
        return new WalletDeductions(subscriptions, installments, reservedBudgets);
    }

    private SubscriptionCacheKey cacheKeyFor(Wallet wallet) {
        SubscriptionState state = wallet.getState() == WalletState.PREVIEW
                ? SubscriptionState.PREVIEW
                : SubscriptionState.PRODUCTION;
        return new SubscriptionCacheKey(wallet.getEffectiveMonth(), state, wallet.getOwnerId());
    }

    private record SubscriptionCacheKey(YearMonth month, SubscriptionState state, String ownerId) {}

    private record OwnerMonthKey(String ownerId, YearMonth month) {}
}
