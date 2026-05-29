package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

public interface FindCreditCardByIdBoundary {

    /**
     * @throws br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException
     *         if no credit card exists with the given id or does not belong to ownerId
     */
    CreditCardOutput findById(String id, String ownerId);
}
