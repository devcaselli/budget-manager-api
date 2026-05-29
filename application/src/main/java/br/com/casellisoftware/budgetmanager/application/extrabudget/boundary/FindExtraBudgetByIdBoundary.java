package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

public interface FindExtraBudgetByIdBoundary {

    ExtraBudgetOutput execute(String id, String ownerId);
}
