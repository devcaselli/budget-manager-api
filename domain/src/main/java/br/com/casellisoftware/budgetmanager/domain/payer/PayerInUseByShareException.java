package br.com.casellisoftware.budgetmanager.domain.payer;

/**
 * Raised when attempting to delete a payer that is still referenced by at
 * least one active share. The share must be reverted first so the
 * accounting trail is preserved.
 */
public class PayerInUseByShareException extends RuntimeException {

    public PayerInUseByShareException(String payerId) {
        super("payer is referenced by an active share; revert the share before deleting payerId=" + payerId);
    }
}
