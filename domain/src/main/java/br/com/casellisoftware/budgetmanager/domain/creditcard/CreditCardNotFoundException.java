package br.com.casellisoftware.budgetmanager.domain.creditcard;

public class CreditCardNotFoundException extends RuntimeException {

    public CreditCardNotFoundException(String creditCardId) {
        super("CreditCard not found: " + creditCardId);
    }
}
