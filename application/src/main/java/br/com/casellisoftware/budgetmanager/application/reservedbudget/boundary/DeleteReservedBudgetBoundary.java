package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

public interface DeleteReservedBudgetBoundary {

    void execute(String id, String ownerId);
}
