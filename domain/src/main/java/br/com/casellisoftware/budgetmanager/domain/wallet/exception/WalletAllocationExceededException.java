package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

public class WalletAllocationExceededException extends RuntimeException {

    public WalletAllocationExceededException(String message) {
        super(message);
    }
}
