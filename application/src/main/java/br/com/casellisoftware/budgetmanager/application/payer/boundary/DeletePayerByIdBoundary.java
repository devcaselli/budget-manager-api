package br.com.casellisoftware.budgetmanager.application.payer.boundary;

public interface DeletePayerByIdBoundary {
    void execute(String id, String ownerId);
}
