package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.flag.FlagStrategy;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.BulletAllocationPolicy;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template Method base for save-bullet strategies. Concrete subclasses implement
 * {@link #reservedSubscriptions(Wallet)} to determine how much of the wallet budget
 * is already spoken for by subscriptions before the bullet allocation is validated.
 */
public abstract class AbstractSaveBulletStrategy implements FlagStrategy<BulletInput, BulletOutput> {

    private static final Logger log = LoggerFactory.getLogger(AbstractSaveBulletStrategy.class);

    protected final BulletRepository bulletRepository;
    protected final WalletRepository walletRepository;
    protected final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    protected final SubscriptionRepository subscriptionRepository;

    protected AbstractSaveBulletStrategy(BulletRepository bulletRepository,
                                         WalletRepository walletRepository,
                                         FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                         SubscriptionRepository subscriptionRepository) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public final BulletOutput apply(BulletInput input) {
        Wallet wallet = findWalletDomainByIdBoundary.findById(input.walletId(), input.ownerId());

        Money budget = Money.of(input.budget());
        Money reservedSubscriptions = reservedSubscriptions(wallet);

        BulletAllocationPolicy.validateAllocation(wallet, budget, reservedSubscriptions);

        Wallet debited = wallet.debit(budget);
        Bullet bullet = Bullet.create(input.description(), budget, budget, wallet.getId(), input.flag(), input.ownerId());

        walletRepository.save(debited);
        Bullet saved = bulletRepository.save(bullet);
        log.info("Bullet saved id={} walletRemaining={}", saved.getId(), debited.getRemaining().amount());

        return BulletOutputAssembler.from(saved);
    }

    /**
     * Returns the amount of the wallet budget that is already reserved by subscriptions.
     * Implementations decide whether to compute the real subscription total or return zero.
     *
     * @param wallet the wallet for which the bullet is being saved
     * @return the reserved subscription amount (never {@code null})
     */
    protected abstract Money reservedSubscriptions(Wallet wallet);
}
