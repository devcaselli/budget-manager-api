package br.com.casellisoftware.budgetmanager.application.payer.boundary;

public interface FindPayerByIdBoundary {
    PayerOutput findById(String id, String ownerId);
}
