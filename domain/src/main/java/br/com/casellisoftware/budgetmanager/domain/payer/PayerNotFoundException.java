package br.com.casellisoftware.budgetmanager.domain.payer;

public class PayerNotFoundException extends RuntimeException {

    public PayerNotFoundException(String payerId) {
        super("Payer not found: " + payerId);
    }
}
