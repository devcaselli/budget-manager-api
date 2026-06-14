package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
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
import java.util.Set;
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

    /**
     * Converts raw repo lists into a {@link WalletDeductions} for the given wallet.
     *
     * <p>Linked exclusion (rule 4): if a subscription or installment is linked to an active
     * reserved budget <em>and</em> that link is applicable for {@code wallet.getEffectiveMonth()}
     * (i.e. {@code link.fromMonth() ≤ month}), it is excluded from the direct deduction
     * calculators. Its cost is already absorbed inside the reserved-budget ceiling — counting it
     * again would double-count against the wallet.</p>
     *
     * <p>Links come embedded in the already-loaded {@code activeReservedBudgets} documents,
     * so no extra query is needed.</p>
     *
     * @implNote Time complexity: O(R·L + S + I) where R = active RB count, L = max links per RB,
     *     S = active subscription count, I = active installment count. Space: O(R·L).
     */
    private WalletDeductions toDeductions(Wallet wallet,
                                          List<Subscription> activeSubscriptions,
                                          List<Installment> activeInstallments,
                                          List<ReservedBudget> activeReservedBudgets) {
        YearMonth month = wallet.getEffectiveMonth();
        Set<String> excludedSubIds = linkedSourceIds(activeReservedBudgets, ReservedBudgetLinkSourceType.SUBSCRIPTION, month);
        Set<String> excludedInstIds = linkedSourceIds(activeReservedBudgets, ReservedBudgetLinkSourceType.INSTALLMENT, month);

        List<Subscription> billableSubscriptions = excludedSubIds.isEmpty()
                ? activeSubscriptions
                : activeSubscriptions.stream()
                        .filter(s -> !excludedSubIds.contains(s.getId()))
                        .toList();

        List<Installment> billableInstallments = excludedInstIds.isEmpty()
                ? activeInstallments
                : activeInstallments.stream()
                        .filter(i -> !excludedInstIds.contains(i.getId()))
                        .toList();

        Money subscriptions = SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, billableSubscriptions, shareRepository);
        Money installments = InstallmentWalletBalanceCalculator.installmentTotal(wallet, billableInstallments, shareRepository);
        Money reservedBudgets = ReservedBudgetWalletBalanceCalculator.reservedBudgetTotal(wallet, activeReservedBudgets);
        return new WalletDeductions(subscriptions, installments, reservedBudgets);
    }

    /**
     * Collects source IDs of the given type whose link is applicable for {@code month}
     * across all active reserved budgets.
     *
     * @implNote Time complexity: O(R·L). Space: O(linked item count).
     */
    private static Set<String> linkedSourceIds(List<ReservedBudget> activeReservedBudgets,
                                                ReservedBudgetLinkSourceType type,
                                                YearMonth month) {
        return activeReservedBudgets.stream()
                .flatMap(rb -> rb.getLinks().stream())
                .filter(link -> link.sourceType() == type && link.isApplicable(month))
                .map(ReservedBudgetLink::sourceId)
                .collect(Collectors.toSet());
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
