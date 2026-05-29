package br.com.casellisoftware.budgetmanager.domain.installment;

/**
 * Thrown when the input for a standalone installment is logically invalid —
 * e.g. both originalValue and installmentValue provided, neither provided,
 * or an unrecognised currency code.
 *
 * <p>Maps to HTTP 400 in {@code GlobalExceptionHandler}.</p>
 */
public class InvalidStandaloneInstallmentInputException extends RuntimeException {

    public InvalidStandaloneInstallmentInputException(String message) {
        super(message);
    }
}
