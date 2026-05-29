package br.com.casellisoftware.budgetmanager.domain.installment;

public class InstallmentAlreadyDeletedException extends RuntimeException {

    public InstallmentAlreadyDeletedException(String installmentId) {
        super("Installment already deleted: " + installmentId);
    }
}
