package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.DeleteExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletPatch;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetAllocation;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Use case: soft-delete an {@link ExtraBudget}, reverting each allocation's bullet.
 *
 * <p>Algorithm (validate-all-then-mutate, O(n), n = allocations):</p>
 * <ol>
 *   <li>Load ExtraBudget; throw {@link ExtraBudgetNotFoundException} if absent.</li>
 *   <li>If already deleted, return idempotently — no writes.</li>
 *   <li>Validate phase: load each bullet, check sufficient remaining.
 *       Accumulate patched bullets in memory. No writes yet.</li>
 *   <li>Mutation phase: persist all patched bullets, then persist markDeleted.</li>
 * </ol>
 *
 * <p>Atomicity guaranteed by {@code TransactionalDeleteExtraBudgetByIdBoundary} decorator.</p>
 *
 * @implNote Time complexity: O(n), Space complexity: O(n) — n = allocations count.
 */
public class DeleteExtraBudgetByIdUseCase implements DeleteExtraBudgetByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteExtraBudgetByIdUseCase.class);

    private final ExtraBudgetRepository extraBudgetRepository;
    private final BulletRepository bulletRepository;

    public DeleteExtraBudgetByIdUseCase(ExtraBudgetRepository extraBudgetRepository,
                                         BulletRepository bulletRepository) {
        this.extraBudgetRepository = Objects.requireNonNull(extraBudgetRepository, "extraBudgetRepository must not be null");
        this.bulletRepository = Objects.requireNonNull(bulletRepository, "bulletRepository must not be null");
    }

    @Override
    public void execute(String id, String ownerId) {
        log.debug("Deleting extra budget id={} ownerId={}", id, ownerId);

        // Step 1 — load
        ExtraBudget extra = extraBudgetRepository.findById(id, ownerId)
                .orElseThrow(() -> new ExtraBudgetNotFoundException(id));

        // Step 2 — idempotent: already deleted
        if (extra.isDeleted()) {
            log.debug("ExtraBudget id={} already deleted, skipping", id);
            return;
        }

        // Step 3 — validate phase: load bullets and compute reverted state (no writes)
        List<Bullet> patchedBullets = new ArrayList<>(extra.getAllocations().size());
        for (ExtraBudgetAllocation alloc : extra.getAllocations()) {
            Bullet bullet = bulletRepository.findById(alloc.bulletId(), ownerId)
                    .orElseGet(() -> {
                        log.warn("rollback: extra-budget delete aborted — bullet not found, bulletId={} extraBudgetId={} ownerId={}",
                                alloc.bulletId(), id, ownerId);
                        throw new BulletNotFoundException(alloc.bulletId());
                    });

            Money newRemaining;
            try {
                newRemaining = bullet.getRemaining().debitBy(alloc.amount());
            } catch (IllegalArgumentException e) {
                log.warn("rollback: extra-budget delete aborted — insufficient remaining on bullet {}, remaining={} allocation={} extraBudgetId={}",
                        bullet.getId(), bullet.getRemaining().amount(), alloc.amount().amount(), id);
                throw new IllegalStateException(
                        "cannot revert: bullet " + bullet.getId() + " has insufficient remaining ("
                                + bullet.getRemaining().amount() + " < " + alloc.amount().amount() + ")", e);
            }

            Money newBudget = bullet.getBudget().debitBy(alloc.amount());

            Bullet patched = bullet.patch(new BulletPatch(
                    Optional.empty(),
                    Optional.of(newBudget),
                    Optional.of(newRemaining),
                    Optional.empty(),
                    Optional.empty()));
            patchedBullets.add(patched);
        }

        // Step 4 — mutation phase: 1 batched bullet write, then mark deleted
        bulletRepository.saveAll(patchedBullets);

        ExtraBudget deleted = extra.markDeleted(LocalDateTime.now());
        extraBudgetRepository.save(deleted);

        log.debug("ExtraBudget deleted id={} allocations reverted={}", id, patchedBullets.size());
    }
}
