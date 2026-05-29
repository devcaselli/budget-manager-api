package br.com.casellisoftware.budgetmanager.domain.expense;

public class ExpenseNotFoundException extends RuntimeException {

    public ExpenseNotFoundException(String expenseId) {
        super("Expense not found: " + expenseId);
    }
}
