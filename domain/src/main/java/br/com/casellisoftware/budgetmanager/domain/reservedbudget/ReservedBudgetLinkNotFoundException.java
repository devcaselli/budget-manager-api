package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

/**
 * Thrown when an unlink operation targets a {@code (sourceType, sourceId)} pair that is
 * not present in the {@link ReservedBudget}'s link list.
 */
public class ReservedBudgetLinkNotFoundException extends RuntimeException {

    private final ReservedBudgetLinkSourceType sourceType;
    private final String sourceId;
    private final String reservedBudgetId;

    public ReservedBudgetLinkNotFoundException(ReservedBudgetLinkSourceType sourceType,
                                               String sourceId,
                                               String reservedBudgetId) {
        super(String.format(
                "Link not found: %s '%s' is not linked to reserved budget '%s'",
                sourceType, sourceId, reservedBudgetId));
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.reservedBudgetId = reservedBudgetId;
    }

    public ReservedBudgetLinkSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getReservedBudgetId() {
        return reservedBudgetId;
    }
}
