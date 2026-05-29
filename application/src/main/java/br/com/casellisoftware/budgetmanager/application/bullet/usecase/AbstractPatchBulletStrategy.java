package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.flag.FlagStrategy;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletPatch;
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
 * Template Method base for patch-bullet strategies. Concrete subclasses implement
 * {@link #reservedSubscriptions(Wallet)} to determine how much of the wallet budget
 * is already spoken for by subscriptions before the bullet reallocation is validated.
 */
public abstract class AbstractPatchBulletStrategy implements FlagStrategy<PatchBulletInput, BulletOutput> {

    private static final Logger log = LoggerFactory.getLogger(AbstractPatchBulletStrategy.class);

    protected final BulletRepository bulletRepository;
    protected final WalletRepository walletRepository;
    protected final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    protected final SubscriptionRepository subscriptionRepository;

    protected AbstractPatchBulletStrategy(BulletRepository bulletRepository,
                                          WalletRepository walletRepository,
                                          FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                          SubscriptionRepository subscriptionRepository) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public final BulletOutput apply(PatchBulletInput input) {
        log.info("Patching bullet id={}", input.id());

        Bullet existing = bulletRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new BulletNotFoundException(input.id()));

        Money requestedBudget = input.budget() == null ? null : Money.of(input.budget());
        Money effectiveBudget = requestedBudget == null ? existing.getBudget() : requestedBudget;
        if (!existing.getBudget().equals(effectiveBudget)) {
            reconcileWallet(existing, effectiveBudget, input.ownerId());
        }

        BulletPatch patch = toPatch(input, existing, requestedBudget);
        if (log.isDebugEnabled()) {
            log.debug("Applying bullet patch id={}, fields={}", input.id(), patch.appliedFieldNames());
        }

        Bullet patched = existing.patch(patch);
        Bullet saved = bulletRepository.save(patched);
        log.info("Bullet patched successfully, id={}", saved.getId());

        return BulletOutputAssembler.from(saved);
    }

    private BulletPatch toPatch(PatchBulletInput input, Bullet existing, Money requestedBudget) {
        Money requestedRemaining = input.remaining() == null ? null : Money.of(input.remaining());

        if (requestedBudget != null && !existing.getBudget().equals(requestedBudget)) {
            requestedRemaining = adjustedRemaining(existing, requestedBudget);
        }

        return BulletPatch.empty()
                .withDescription(input.description())
                .withBudget(requestedBudget)
                .withRemaining(requestedRemaining)
                .withWalletId(input.walletId());
    }

    private Money adjustedRemaining(Bullet existing, Money requestedBudget) {
        if (requestedBudget.isGreaterThan(existing.getBudget())) {
            return existing.getRemaining().add(requestedBudget.subtract(existing.getBudget()));
        }
        return existing.getRemaining().debitBy(existing.getBudget().subtract(requestedBudget));
    }

    private void reconcileWallet(Bullet existing, Money requestedBudget, String ownerId) {
        Wallet wallet = findWalletDomainByIdBoundary.findById(existing.getWalletId(), ownerId);
        Money reservedSubscriptions = reservedSubscriptions(wallet);

        BulletAllocationPolicy.validateReallocation(
                wallet,
                existing.getBudget(),
                requestedBudget,
                existing.getRemaining(),
                reservedSubscriptions
        );

        Wallet reconciled;
        if (requestedBudget.isGreaterThan(existing.getBudget())) {
            reconciled = wallet.debit(requestedBudget.subtract(existing.getBudget()));
        } else {
            reconciled = wallet.credit(existing.getBudget().subtract(requestedBudget));
        }

        walletRepository.save(reconciled);
    }

    /**
     * Returns the amount of the wallet budget that is already reserved by subscriptions.
     * Implementations decide whether to compute the real subscription total or return zero.
     *
     * @param wallet the wallet owning the bullet being patched
     * @return the reserved subscription amount (never {@code null})
     */
    protected abstract Money reservedSubscriptions(Wallet wallet);
}
