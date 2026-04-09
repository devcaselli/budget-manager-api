package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/expenses")
public interface SaveExpenseEntrypoint {


    @PostMapping
    ResponseEntity<ExpenseResponseDto> save(@Valid @RequestBody ExpenseRequestDto expenseRequestDto);
}
