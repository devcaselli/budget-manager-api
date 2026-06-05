package br.com.casellisoftware.budgetmanager.application.subscriptioncharge.usecase;

import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.FindSubscriptionChargesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletSubscriptionSelector;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class FindSubscriptionChargesByWalletIdUseCase implements FindSubscriptionChargesByWalletIdBoundary {

    private final SubscriptionRepository subscriptionRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    private final ShareRepository shareRepository;

    public FindSubscriptionChargesByWalletIdUseCase(SubscriptionRepository subscriptionRepository,
                                                    FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                    ShareRepository shareRepository) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary, "findWalletDomainByIdBoundary must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    public List<SubscriptionChargeOutput> execute(String walletId, String ownerId) {
        Wallet wallet = findWalletDomainByIdBoundary.findById(walletId, ownerId);

        return WalletSubscriptionSelector.activeForWallet(subscriptionRepository, wallet)
                .stream()
                .map(subscription -> enrichWithShare(
                        SubscriptionChargeOutputAssembler.preview(wallet, subscription), subscription, wallet))
                .toList();
    }

    private SubscriptionChargeOutput enrichWithShare(SubscriptionChargeOutput preview, Subscription subscription, Wallet wallet) {
        Optional<Share> activeShare = shareRepository.findActiveBySourceId(
                ShareSourceType.SUBSCRIPTION, subscription.getId(), subscription.getOwnerId());
        if (activeShare.isEmpty() || !activeShare.get().isEffectiveFor(wallet.getEffectiveMonth())) {
            return preview;
        }
        Share share = activeShare.get();
        BigDecimal effectiveOwner = preview.amount()
                .multiply(share.getOwnerRatio())
                .setScale(2, RoundingMode.HALF_EVEN);
        return new SubscriptionChargeOutput(
                preview.id(),
                preview.subscriptionId(),
                preview.walletId(),
                preview.month(),
                preview.amount(),
                preview.remaining(),
                preview.flag(),
                true,
                effectiveOwner
        );
    }
}
