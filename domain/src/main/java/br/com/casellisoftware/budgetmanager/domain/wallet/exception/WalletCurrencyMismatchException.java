package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

public class WalletCurrencyMismatchException extends RuntimeException {

    public WalletCurrencyMismatchException(String message) {
        super(message);
    }
}
