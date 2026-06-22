package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Input for linking a subscription or installment to a reserved budget.
 *
 * @param reservedBudgetId the target reserved budget id (never blank)
 * @param sourceType       SUBSCRIPTION or INSTALLMENT
 * @param sourceId         the subscription/installment id (never blank)
 * @param fromMonth        the month from which the link takes effect (never null)
 * @param ownerId          owner scope for multi-tenant isolation (never blank)
 */
public record LinkReservedBudgetSourceInput(
        String reservedBudgetId,
        ReservedBudgetLinkSourceType sourceType,
        String sourceId,
        YearMonth fromMonth,
        String ownerId
) {
    public LinkReservedBudgetSourceInput {
        Objects.requireNonNull(reservedBudgetId, "reservedBudgetId must not be null");
        if (reservedBudgetId.isBlank()) throw new IllegalArgumentException("reservedBudgetId must not be blank");
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        if (sourceId.isBlank()) throw new IllegalArgumentException("sourceId must not be blank");
        Objects.requireNonNull(fromMonth, "fromMonth must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        if (ownerId.isBlank()) throw new IllegalArgumentException("ownerId must not be blank");
    }
}
