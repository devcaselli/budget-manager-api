package br.com.casellisoftware.budgetmanager.domain.payment;

public class WalletMismatchException extends RuntimeException {

    public WalletMismatchException(String message) {
        super(message);
    }
}
