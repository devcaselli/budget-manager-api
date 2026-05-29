package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;

public record CreditCardInput(String name, String ownerId) {

    public CreditCardInput(String name) {
        this(name, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public CreditCardInput withOwnerId(String ownerId) {
        return new CreditCardInput(name, ownerId);
    }
}
