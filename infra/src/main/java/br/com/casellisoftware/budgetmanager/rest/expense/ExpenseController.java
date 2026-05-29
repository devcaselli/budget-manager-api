package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindAllMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpensesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.PagedExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.mappers.ExpenseRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

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
    private final FindMineExpensesBoundary findMineExpensesBoundary;
    private final FindAllMineExpensesBoundary findAllMineExpensesBoundary;
    private final DeleteExpenseByIdBoundary deleteExpenseByIdBoundary;
    private final ExpenseRestMapper mapper;

    @PostMapping
    public ResponseEntity<ExpenseResponseDto> save(@Valid @RequestBody ExpenseRequestDto request,
                                                   AuthenticatedUser authenticatedUser) {
        ExpenseOutput output = saveExpenseBoundary.execute(
                mapper.expenseRequestDtoToExpenseInput(request).withOwnerId(authenticatedUser.ownerId())
        );

        ExpenseResponseDto response = mapper.expenseOutputToExpenseResponseDto(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ExpenseResponseDto>> findMine(
            @RequestParam(defaultValue = "12") @Pattern(regexp = "12|24", message = "months must be 12 or 24") String months,
            AuthenticatedUser authenticatedUser) {

        List<ExpenseResponseDto> response = findMineExpensesBoundary
                .execute(Integer.parseInt(months), authenticatedUser.ownerId())
                .stream()
                .map(mapper::expenseOutputToExpenseResponseDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine/export")
    public ResponseEntity<String> exportMine(AuthenticatedUser authenticatedUser) {
        List<ExpenseResponseDto> expenses = findAllMineExpensesBoundary.execute(authenticatedUser.ownerId())
                .stream()
                .map(mapper::expenseOutputToExpenseResponseDto)
                .toList();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("expenses.csv").build().toString())
                .body(ExpenseCsvExporter.export(expenses));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<PagedExpenseResponseDto> findByWalletId(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "false") boolean unhidden,
            AuthenticatedUser authenticatedUser) {

        PageResult<ExpenseOutput> result = findExpensesByWalletIdBoundary.execute(walletId, page, size, unhidden, authenticatedUser.ownerId());
        PagedExpenseResponseDto response = mapper.toPagedResponse(result);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpenseById(@PathVariable String id,
                                                  AuthenticatedUser authenticatedUser){
        this.deleteExpenseByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }
}
