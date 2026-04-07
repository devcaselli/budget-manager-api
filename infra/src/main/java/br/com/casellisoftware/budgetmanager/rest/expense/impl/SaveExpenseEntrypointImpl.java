package br.com.casellisoftware.budgetmanager.rest.expense.impl;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.mappers.expense.ExpenseMappers;
import br.com.casellisoftware.budgetmanager.rest.expense.SaveExpenseEntrypoint;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SaveExpenseEntrypointImpl implements SaveExpenseEntrypoint {

    private final SaveExpenseBoundary saveExpenseBoundary;
    private final ExpenseMappers mappers;

    @Override
    public ResponseEntity<ExpenseResponseDto> save(ExpenseRequestDto expenseRequestDto) {
        ExpenseOutput output = this.saveExpenseBoundary.execute(
                this.mappers.expenseRequestDtoToExpenseInput(expenseRequestDto)
        );

        ExpenseResponseDto response = this.mappers.expenseOutputToExpenseResponseDto(output);

        return ResponseEntity.status(201).body(response);
    }
}
