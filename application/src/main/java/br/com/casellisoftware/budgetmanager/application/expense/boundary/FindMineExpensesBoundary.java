package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import java.util.List;

public interface FindMineExpensesBoundary {

    List<ExpenseOutput> execute(int months, String ownerId);
}
