package br.com.casellisoftware.budgetmanager.domain.installment;

/**
 * Thrown when a patch request for an installment is logically invalid —
 * e.g. both originalValue and installmentValue provided simultaneously.
 *
 * <p>Maps to HTTP 400 in {@code GlobalExceptionHandler}.</p>
 */
public class InvalidInstallmentPatchException extends RuntimeException {

    public InvalidInstallmentPatchException(String message) {
        super(message);
    }
}
