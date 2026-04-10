package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.mappers.ExpenseRestMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Single REST controller for all expense operations. Each method delegates to
 * the corresponding application-layer use case and uses {@link ExpenseRestMapper}
 * to translate between HTTP DTOs and application boundary records.
 */
@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final SaveExpenseBoundary saveExpenseBoundary;
    private final ExpenseRestMapper mapper;

    public ExpenseController(SaveExpenseBoundary saveExpenseBoundary, ExpenseRestMapper mapper) {
        this.saveExpenseBoundary = saveExpenseBoundary;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponseDto> save(@Valid @RequestBody ExpenseRequestDto request) {
        ExpenseOutput output = saveExpenseBoundary.execute(
                mapper.expenseRequestDtoToExpenseInput(request)
        );

        ExpenseResponseDto response = mapper.expenseOutputToExpenseResponseDto(output);

        return ResponseEntity.created(URI.create("/expenses/" + response.id())).body(response);
    }
}
