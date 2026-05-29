package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import java.util.List;

public interface FindAllMineExpensesBoundary {

    List<ExpenseOutput> execute(String ownerId);
}
