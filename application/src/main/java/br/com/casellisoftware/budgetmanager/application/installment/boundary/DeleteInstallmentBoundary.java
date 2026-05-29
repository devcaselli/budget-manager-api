package br.com.casellisoftware.budgetmanager.application.installment.boundary;

public interface DeleteInstallmentBoundary {

    void execute(String id, String ownerId);
}
