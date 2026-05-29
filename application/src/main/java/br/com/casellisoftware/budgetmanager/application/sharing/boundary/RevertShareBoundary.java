package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

public interface RevertShareBoundary {

    void execute(String shareId, String ownerId);
}
