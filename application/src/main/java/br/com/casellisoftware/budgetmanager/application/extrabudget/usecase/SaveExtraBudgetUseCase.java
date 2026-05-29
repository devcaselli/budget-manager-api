package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.SaveExtraBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletPatch;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.BulletNotInWalletException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetAllocation;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case: save a new {@link ExtraBudget}, crediting each allocation's bullet.
 *
 * <p>Algorithm (O(n), n = number of allocations):</p>
 * <ol>
 *   <li>Validate wallet exists for ownerId.</li>
 *   <li>Load and validate each bullet (owner + wallet membership).</li>
 *   <li>Build domain entity — invariants validated by {@code ExtraBudget.create}.</li>
 *   <li>Credit each bullet's budget and remaining by its allocation amount.</li>
 *   <li>Persist all bullets, then persist the extra-budget.</li>
 * </ol>
 *
 * <p>Atomicity is guaranteed by the {@code TransactionalSaveExtraBudgetBoundary} decorator.</p>
 *
 * @implNote Time complexity: O(n), Space complexity: O(n) — n = allocations count.
 */
public class SaveExtraBudgetUseCase implements SaveExtraBudgetBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveExtraBudgetUseCase.class);

    private final ExtraBudgetRepository extraBudgetRepository;
    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;

    public SaveExtraBudgetUseCase(ExtraBudgetRepository extraBudgetRepository,
                                   BulletRepository bulletRepository,
                                   WalletRepository walletRepository) {
        this.extraBudgetRepository = Objects.requireNonNull(extraBudgetRepository, "extraBudgetRepository must not be null");
        this.bulletRepository = Objects.requireNonNull(bulletRepository, "bulletRepository must not be null");
        this.walletRepository = Objects.requireNonNull(walletRepository, "walletRepository must not be null");
    }

    @Override
    public ExtraBudgetOutput execute(ExtraBudgetInput input) {
        log.debug("Saving extra budget: walletId={} ownerId={} allocations={}",
                input.walletId(), input.ownerId(), input.allocations().size());

        // Step 1 — validate wallet exists
        walletRepository.findById(input.walletId(), input.ownerId())
                .orElseThrow(() -> new WalletNotFoundException(input.walletId()));

        // Step 2 — load and validate each bullet; O(n) reads
        Map<String, Bullet> bulletsById = new HashMap<>(input.allocations().size());
        for (AllocationInput alloc : input.allocations()) {
            Bullet bullet = bulletRepository.findById(alloc.bulletId(), input.ownerId())
                    .orElseGet(() -> {
                        log.warn("rollback: extra-budget save aborted — bullet not found, bulletId={} walletId={} ownerId={}",
                                alloc.bulletId(), input.walletId(), input.ownerId());
                        throw new BulletNotFoundException(alloc.bulletId());
                    });
            if (!bullet.getWalletId().equals(input.walletId())) {
                log.warn("rollback: extra-budget save aborted — bullet {} does not belong to wallet {}, ownerId={}",
                        bullet.getId(), input.walletId(), input.ownerId());
                throw new BulletNotInWalletException(bullet.getId(), input.walletId());
            }
            bulletsById.put(bullet.getId(), bullet);
        }

        // Step 3 — build domain allocations and entity (invariants validated inside create)
        List<ExtraBudgetAllocation> domainAllocations = input.allocations().stream()
                .map(a -> new ExtraBudgetAllocation(a.bulletId(), Money.of(a.amount())))
                .toList();

        ExtraBudget extra = ExtraBudget.create(
                input.ownerId(),
                input.description(),
                input.walletId(),
                Money.of(input.amount()),
                domainAllocations);

        // Step 4 — credit each bullet; 1 batched write
        List<Bullet> patchedBullets = extra.getAllocations().stream()
                .map(alloc -> {
                    Bullet b = bulletsById.get(alloc.bulletId());
                    Money newBudget    = b.getBudget().creditBy(alloc.amount());
                    Money newRemaining = b.getRemaining().creditBy(alloc.amount());
                    return b.patch(new BulletPatch(
                            Optional.empty(),
                            Optional.of(newBudget),
                            Optional.of(newRemaining),
                            Optional.empty(),
                            Optional.empty()));
                })
                .toList();
        bulletRepository.saveAll(patchedBullets);

        // Step 5 — persist extra budget
        ExtraBudget saved = extraBudgetRepository.save(extra);
        log.debug("ExtraBudget saved: id={}", saved.getId());

        return ExtraBudgetOutputAssembler.from(saved);
    }
}
