package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

public interface DeleteExtraBudgetByIdBoundary {

    void execute(String id, String ownerId);
}
