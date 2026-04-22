package br.com.casellisoftware.budgetmanager.domain.payment;

public class CurrencyMismatchException extends RuntimeException {

    public CurrencyMismatchException(String message) {
        super(message);
    }
}
