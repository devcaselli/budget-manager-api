package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindWalletSharesBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletSubscriptionSelector;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists the recurring shares visible on a wallet's share screen. A subscription
 * or installment recurs across monthly wallets, so a share created in an earlier
 * wallet must still surface in a later one — until it is stopped from that month
 * onward (see {@link Share#isEffectiveFor(YearMonth)}).
 *
 * <p>O(1) DB round-trips: 1 wallet read, 1 subscription query, 1 installment
 * query, 2 batch share queries, 1 batch payer-name query — independent of the
 * number of active sources.</p>
 */
public class FindWalletSharesUseCase implements FindWalletSharesBoundary {

    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    private final SubscriptionRepository subscriptionRepository;
    private final InstallmentRepository installmentRepository;
    private final ShareRepository shareRepository;
    private final PayerRepository payerRepository;

    public FindWalletSharesUseCase(FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                   SubscriptionRepository subscriptionRepository,
                                   InstallmentRepository installmentRepository,
                                   ShareRepository shareRepository,
                                   PayerRepository payerRepository) {
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary, "findWalletDomainByIdBoundary must not be null");
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.payerRepository = Objects.requireNonNull(payerRepository, "payerRepository must not be null");
    }

    @Override
    public List<ShareOutput> execute(String walletId, String ownerId) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        Wallet wallet = findWalletDomainByIdBoundary.findById(walletId, ownerId);
        YearMonth month = wallet.getEffectiveMonth();

        List<String> subscriptionIds = WalletSubscriptionSelector.activeForWallet(subscriptionRepository, wallet).stream()
                .map(Subscription::getId)
                .toList();
        List<String> installmentIds = installmentRepository.findActiveAffecting(month, ownerId).stream()
                .filter(installment -> InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, wallet))
                .map(Installment::getId)
                .toList();

        Map<String, Share> subShares = subscriptionIds.isEmpty()
                ? Map.of()
                : shareRepository.findActiveBySourceIds(ShareSourceType.SUBSCRIPTION, subscriptionIds, ownerId);
        Map<String, Share> instShares = installmentIds.isEmpty()
                ? Map.of()
                : shareRepository.findActiveBySourceIds(ShareSourceType.INSTALLMENT, installmentIds, ownerId);

        List<Share> effectiveShares = new ArrayList<>(subShares.size() + instShares.size());
        addEffective(effectiveShares, subShares.values(), month);
        addEffective(effectiveShares, instShares.values(), month);
        if (effectiveShares.isEmpty()) {
            return List.of();
        }

        Map<String, String> payerNameById = resolvePayerNames(effectiveShares, ownerId);
        return effectiveShares.stream()
                .map(share -> ShareOutputAssembler.from(share, payerNameById))
                .toList();
    }

    private static void addEffective(List<Share> target, Iterable<Share> shares, YearMonth month) {
        for (Share share : shares) {
            if (share.isEffectiveFor(month)) {
                target.add(share);
            }
        }
    }

    private Map<String, String> resolvePayerNames(List<Share> shares, String ownerId) {
        Set<String> payerIds = shares.stream()
                .flatMap(share -> share.getQuotas().stream())
                .map(ShareQuota::payerId)
                .collect(Collectors.toSet());
        if (payerIds.isEmpty()) {
            return Map.of();
        }
        Map<String, String> payerNameById = new HashMap<>(payerIds.size());
        for (Payer payer : payerRepository.findAllByIdsIn(payerIds, ownerId)) {
            payerNameById.put(payer.getId(), payer.getName());
        }
        return payerNameById;
    }
}
