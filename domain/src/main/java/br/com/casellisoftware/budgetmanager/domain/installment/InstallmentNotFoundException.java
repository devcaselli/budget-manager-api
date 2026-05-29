package br.com.casellisoftware.budgetmanager.domain.installment;

public class InstallmentNotFoundException extends RuntimeException {

    public InstallmentNotFoundException(String installmentId) {
        super("Installment not found: " + installmentId);
    }
}
