package br.com.casellisoftware.budgetmanager.application.expense.boundary;

public interface PatchExpenseBoundary {

    ExpenseOutput execute(PatchExpenseInput input);
}
