package br.com.casellisoftware.budgetmanager.persistence.expense.mappers;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpensePersistenceMapper {

    ExpenseDocument expenseDomainToExpenseDocument(Expense expense);
    Expense expenseDocumentToExpense(ExpenseDocument expenseDocument);
}
