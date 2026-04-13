package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpensesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.PagedExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.mappers.ExpenseRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Single REST controller for all expense operations. Each method delegates to
 * the corresponding application-layer use case and uses {@link ExpenseRestMapper}
 * to translate between HTTP DTOs and application boundary records.
 */
@Validated
@RestController
@RequestMapping("/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final SaveExpenseBoundary saveExpenseBoundary;
    private final FindExpensesByWalletIdBoundary findExpensesByWalletIdBoundary;
    private final DeleteExpenseByIdBoundary deleteExpenseByIdBoundary;
    private final ExpenseRestMapper mapper;

    @PostMapping
    public ResponseEntity<ExpenseResponseDto> save(@Valid @RequestBody ExpenseRequestDto request) {
        ExpenseOutput output = saveExpenseBoundary.execute(
                mapper.expenseRequestDtoToExpenseInput(request)
        );

        ExpenseResponseDto response = mapper.expenseOutputToExpenseResponseDto(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<PagedExpenseResponseDto> findByWalletId(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {

        PageResult<ExpenseOutput> result = findExpensesByWalletIdBoundary.execute(walletId, page, size);
        PagedExpenseResponseDto response = mapper.toPagedResponse(result);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpenseById(@PathVariable String id){
        this.deleteExpenseByIdBoundary.execute(id);
        return ResponseEntity.noContent().build();
    }
}

