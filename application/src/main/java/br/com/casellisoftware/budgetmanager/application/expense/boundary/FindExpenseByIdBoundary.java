package br.com.casellisoftware.budgetmanager.application.expense.boundary;

public interface FindExpenseByIdBoundary {

    ExpenseOutput execute(String id);
}
