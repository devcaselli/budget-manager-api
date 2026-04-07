package br.com.casellisoftware.budgetmanager.rest.expense.mappers;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExpenseRestMapper {

    ExpenseInput expenseRequestDtoToExpenseInput(ExpenseRequestDto expenseRequestDto);
    ExpenseResponseDto expenseOutputToExpenseResponseDto(ExpenseOutput expense);
}
