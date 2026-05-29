package br.com.casellisoftware.budgetmanager.domain.sharing;

/**
 * Raised when attempting to delete an expense, subscription, or installment
 * that is still referenced by an active share. The share must be reverted
 * first so the accounting trail (payments, payer quotas, wallet remaining)
 * stays consistent.
 */
public class SourceInUseByShareException extends RuntimeException {

    private final ShareSourceType sourceType;
    private final String sourceId;

    public SourceInUseByShareException(ShareSourceType sourceType, String sourceId) {
        super(sourceType.name().toLowerCase()
                + " " + sourceId
                + " is referenced by an active share; revert the share before deleting");
        this.sourceType = sourceType;
        this.sourceId = sourceId;
    }

    public ShareSourceType getSourceType() {
        return sourceType;
    }

    public String getSourceId() {
        return sourceId;
    }
}
