package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

/**
 * Thrown when a source item (subscription or installment) is already linked to a
 * <em>different</em> {@link ReservedBudget} owned by the same owner.
 *
 * <p>Cardinality invariant: one item → at most one reserved budget (rule 3).</p>
 */
public class ReservedBudgetLinkConflictException extends RuntimeException {

    private final ReservedBudgetLinkSourceType sourceType;
    private final String sourceId;
    private final String conflictingReservedBudgetId;

    public ReservedBudgetLinkConflictException(ReservedBudgetLinkSourceType sourceType,
                                               String sourceId,
                                               String conflictingReservedBudgetId) {
        super(String.format(
                "%s '%s' is already linked to reserved budget '%s'",
                sourceType, sourceId, conflictingReservedBudgetId));
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.conflictingReservedBudgetId = conflictingReservedBudgetId;
    }

    public ReservedBudgetLinkSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getConflictingReservedBudgetId() {
        return conflictingReservedBudgetId;
    }
}
