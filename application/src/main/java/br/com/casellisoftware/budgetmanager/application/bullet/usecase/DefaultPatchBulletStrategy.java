package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.SubscriptionWalletBalanceCalculator;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletSubscriptionSelector;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;

import java.util.Objects;

public class DefaultPatchBulletStrategy extends AbstractPatchBulletStrategy {

    private final ShareRepository shareRepository;

    public DefaultPatchBulletStrategy(BulletRepository bulletRepository,
                                      WalletRepository walletRepository,
                                      FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                      SubscriptionRepository subscriptionRepository,
                                      ShareRepository shareRepository) {
        super(bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository);
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
    }

    @Override
    protected Money reservedSubscriptions(Wallet wallet) {
        return SubscriptionWalletBalanceCalculator.subscriptionTotal(
                wallet,
                WalletSubscriptionSelector.activeForWallet(subscriptionRepository, wallet),
                shareRepository);
    }
}
