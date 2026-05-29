package br.com.casellisoftware.budgetmanager.application.installment.boundary;

public interface FindInstallmentByIdBoundary {

    InstallmentOutput findById(String id, String ownerId);
}
