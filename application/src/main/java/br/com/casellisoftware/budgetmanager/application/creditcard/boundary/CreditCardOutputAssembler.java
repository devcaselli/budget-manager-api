package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;

public final class CreditCardOutputAssembler {

    private CreditCardOutputAssembler() {
    }

    public static CreditCardOutput from(CreditCard creditCard) {
        return new CreditCardOutput(creditCard.getId(), creditCard.getName(), creditCard.getLabels());
    }
}
