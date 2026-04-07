package br.com.casellisoftware.budgetmanager.application.expense.boundary;

public interface SaveExpenseBoundary {

    ExpenseOutput execute(ExpenseInput input);
}
