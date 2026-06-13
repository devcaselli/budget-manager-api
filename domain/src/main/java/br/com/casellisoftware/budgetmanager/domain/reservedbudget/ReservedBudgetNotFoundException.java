package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

public class ReservedBudgetNotFoundException extends RuntimeException {
    public ReservedBudgetNotFoundException(String id) {
        super("ReservedBudget not found: " + id);
    }
}
