package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;

import java.math.BigDecimal;

public class SaveBulletIgnoreSubscriptionReservationStrategy extends AbstractSaveBulletStrategy {

    public SaveBulletIgnoreSubscriptionReservationStrategy(BulletRepository bulletRepository,
                                                           WalletRepository walletRepository,
                                                           FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                           SubscriptionRepository subscriptionRepository) {
        super(bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository);
    }

    @Override
    protected Money reservedSubscriptions(Wallet wallet) {
        return Money.of(BigDecimal.ZERO, wallet.getBudget().currency());
    }
}
