package br.com.casellisoftware.budgetmanager.mappers.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.persistence.expense.ExpenseDocument;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseMappers {

    ExpenseDocument expenseDomainToExpenseDocument(Expense expense);
    Expense expenseDocumentToExpense(ExpenseDocument expenseDocument);
    Expense expenseRequestToExpense(ExpenseRequestDto expenseRequestDto);
    ExpenseResponseDto  expenseToExpenseResponseDto(Expense expense);
    Expense expenseInputToExpenseDomain(ExpenseInput expenseInput);
    ExpenseOutput expenseDomainToExpenseOutput(Expense expense);
    ExpenseInput expenseRequestDtoToExpenseInput(ExpenseRequestDto expenseRequestDto);
    ExpenseResponseDto expenseOutputToExpenseResponseDto(ExpenseOutput expense);
}
