package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

public interface FindReservedBudgetByIdBoundary {

    ReservedBudgetOutput execute(String id, String ownerId);
}
