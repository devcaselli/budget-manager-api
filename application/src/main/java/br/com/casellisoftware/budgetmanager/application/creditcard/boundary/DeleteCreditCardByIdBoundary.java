package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

public interface DeleteCreditCardByIdBoundary {
    void execute(String id, String ownerId);
}
