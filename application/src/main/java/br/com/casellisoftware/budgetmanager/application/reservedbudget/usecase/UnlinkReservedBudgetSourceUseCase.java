package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.UnlinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.UnlinkReservedBudgetSourceInput;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Removes a link from a reserved budget.
 *
 * <p>No cap re-validation is needed: removing a link only decreases the sum
 * of linked amounts, so the cap invariant is preserved by definition.</p>
 *
 * <p>Returns 404 if the RB does not exist or does not contain the specified link.</p>
 */
public class UnlinkReservedBudgetSourceUseCase implements UnlinkReservedBudgetSourceBoundary {

    private static final Logger log = LoggerFactory.getLogger(UnlinkReservedBudgetSourceUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;

    public UnlinkReservedBudgetSourceUseCase(ReservedBudgetRepository reservedBudgetRepository) {
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository);
    }

    @Override
    public ReservedBudgetOutput execute(UnlinkReservedBudgetSourceInput input) {
        Objects.requireNonNull(input, "input must not be null");
        log.info("Unlinking {} '{}' from reserved budget '{}'",
                input.sourceType(), input.sourceId(), input.reservedBudgetId());

        ReservedBudget rb = reservedBudgetRepository.findById(input.reservedBudgetId(), input.ownerId())
                .orElseThrow(() -> new ReservedBudgetNotFoundException(input.reservedBudgetId()));

        rb.findLink(input.sourceType(), input.sourceId())
                .orElseThrow(() -> new ReservedBudgetLinkNotFoundException(
                        input.sourceType(), input.sourceId(), input.reservedBudgetId()));

        ReservedBudget updated = rb.removeLink(input.sourceType(), input.sourceId());
        ReservedBudget saved = reservedBudgetRepository.save(updated);
        log.info("Link removed: {} '{}' from reserved budget '{}'",
                input.sourceType(), input.sourceId(), saved.getId());

        return ReservedBudgetOutputAssembler.from(saved);
    }
}
