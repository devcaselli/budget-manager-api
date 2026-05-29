package br.com.casellisoftware.budgetmanager.domain.payment;

public class AmountExceedsRemainingException extends RuntimeException {

    public AmountExceedsRemainingException(String message) {
        super(message);
    }
}
