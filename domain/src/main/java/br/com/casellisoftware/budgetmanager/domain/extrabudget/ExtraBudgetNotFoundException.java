package br.com.casellisoftware.budgetmanager.domain.extrabudget;

public class ExtraBudgetNotFoundException extends RuntimeException {
    public ExtraBudgetNotFoundException(String id) {
        super("ExtraBudget not found: " + id);
    }
}
