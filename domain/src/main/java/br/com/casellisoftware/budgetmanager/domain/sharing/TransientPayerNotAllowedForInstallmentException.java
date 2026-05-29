package br.com.casellisoftware.budgetmanager.domain.sharing;

/**
 * Raised when an installment share is created with a quota that resolves to
 * a TRANSIENT payer. Installments cross wallet boundaries (the share lives
 * for the whole payment window), so the quotas must reference STANDING
 * payers — TRANSIENT payers are scoped to a single wallet and would lose
 * their context when the installment materializes in subsequent months.
 */
public class TransientPayerNotAllowedForInstallmentException extends RuntimeException {

    public TransientPayerNotAllowedForInstallmentException(String payerId) {
        super("transient payers are not allowed for installment shares; payerId=" + payerId);
    }
}
