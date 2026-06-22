package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.DeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

public class DeleteReservedBudgetUseCase implements DeleteReservedBudgetBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteReservedBudgetUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;
    private final Clock clock;

    public DeleteReservedBudgetUseCase(ReservedBudgetRepository reservedBudgetRepository, Clock clock) {
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository, "reservedBudgetRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void execute(String id, String ownerId) {
        log.info("Deleting reserved budget id={} ownerId={}", id, ownerId);

        ReservedBudget existing = reservedBudgetRepository.findById(id, ownerId)
                .orElseThrow(() -> new ReservedBudgetNotFoundException(id));

        if (existing.isDeleted()) {
            log.debug("Reserved budget id={} already deleted, skipping", id);
            return;
        }

        ReservedBudget deleted = existing.markDeleted(LocalDateTime.now(clock));
        reservedBudgetRepository.save(deleted);
        log.info("Reserved budget soft-deleted id={}", deleted.getId());
    }
}
