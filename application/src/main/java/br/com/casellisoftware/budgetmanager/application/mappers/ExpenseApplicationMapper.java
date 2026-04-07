package br.com.casellisoftware.budgetmanager.application.mappers;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;

public interface ExpenseApplicationMapper {

    Expense mapToDomain(ExpenseInput domain);
    ExpenseOutput mapToOutput(Expense domain);
}
