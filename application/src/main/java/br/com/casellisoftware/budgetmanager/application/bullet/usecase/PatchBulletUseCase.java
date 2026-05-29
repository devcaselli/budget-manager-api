package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.flag.FlagAwareExecutor;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Patch-bullet use case. Routes through a {@link FlagAwareExecutor} so that the same
 * flag-based strategy selection that governs save-bullet also governs patch-bullet,
 * ensuring consistent {@code BULLET_IGNORE_SUBSCRIPTION_RESERVATION} semantics across
 * both write operations.
 */
public class PatchBulletUseCase implements PatchBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final FlagAwareExecutor<PatchBulletInput, BulletOutput> executor;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public PatchBulletUseCase(BulletRepository bulletRepository,
                              FlagAwareExecutor<PatchBulletInput, BulletOutput> executor,
                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = Objects.requireNonNull(bulletRepository, "bulletRepository must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary, "findWalletDomainByIdBoundary must not be null");
    }

    @Override
    public BulletOutput execute(PatchBulletInput input) {
        log.info("Dispatching patch-bullet id={}", input.id());
        // First read: resolve the wallet flag needed to select the correct strategy.
        // The selected strategy will perform a second read of the same bullet for the
        // actual mutation. Both reads execute within the same @Transactional boundary
        // (see TransactionalPatchBulletBoundary), so the strategy always sees a
        // consistent snapshot. Collapsing these two reads would require passing the
        // pre-fetched Bullet through the FlagAwareExecutor/FlagStrategy interface,
        // which would widen the contract beyond what is warranted at this time.
        Bullet existing = bulletRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new BulletNotFoundException(input.id()));
        Wallet wallet = findWalletDomainByIdBoundary.findById(existing.getWalletId(), input.ownerId());
        return executor.execute(wallet.getFlag(), input);
    }
}
