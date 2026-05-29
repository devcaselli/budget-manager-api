package br.com.casellisoftware.budgetmanager.domain.sharing;

public class ShareCurrencyMismatchException extends RuntimeException {

    public ShareCurrencyMismatchException(String message) {
        super(message);
    }
}
