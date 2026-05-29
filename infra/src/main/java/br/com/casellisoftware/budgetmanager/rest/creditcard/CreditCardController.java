package br.com.casellisoftware.budgetmanager.rest.creditcard;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardChargesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardExpensesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.DeleteCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardChargesBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindAllCreditCardsBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.SaveCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardChargesResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardRequestDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardExpensesResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.PagedCreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.mappers.CreditCardRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;

@Validated
@RestController
@RequestMapping("/credit-cards")
@RequiredArgsConstructor
public class CreditCardController {

    private final SaveCreditCardBoundary saveCreditCardBoundary;
    private final FindCreditCardByIdBoundary findCreditCardByIdBoundary;
    private final FindAllCreditCardsBoundary findAllCreditCardsBoundary;
    private final FindCreditCardExpensesBoundary findCreditCardExpensesBoundary;
    private final FindCreditCardChargesBoundary findCreditCardChargesBoundary;
    private final DeleteCreditCardByIdBoundary deleteCreditCardByIdBoundary;
    private final PatchCreditCardBoundary patchCreditCardBoundary;
    private final CreditCardRestMapper mapper;

    @PostMapping
    public ResponseEntity<CreditCardResponseDto> save(@Valid @RequestBody CreditCardRequestDto request,
                                                      AuthenticatedUser authenticatedUser) {
        CreditCardOutput output = saveCreditCardBoundary.execute(
                mapper.creditCardRequestDtoToCreditCardInput(request).withOwnerId(authenticatedUser.ownerId()));
        CreditCardResponseDto response = mapper.creditCardOutputToCreditCardResponseDto(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditCardResponseDto> findById(@PathVariable String id,
                                                          AuthenticatedUser authenticatedUser) {
        CreditCardOutput output = findCreditCardByIdBoundary.findById(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.creditCardOutputToCreditCardResponseDto(output));
    }

    @GetMapping
    public ResponseEntity<PagedCreditCardResponseDto> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            AuthenticatedUser authenticatedUser) {

        PageResult<CreditCardOutput> result = findAllCreditCardsBoundary.execute(page, size, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toPagedResponse(result));
    }

    @GetMapping("/{id}/expenses")
    public ResponseEntity<CreditCardExpensesResponseDto> findExpensesByCreditCardId(
            @PathVariable String id,
            @RequestParam(required = false) @Pattern(regexp = "^\\d{4}-\\d{2}$") String effectiveMonth,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            AuthenticatedUser authenticatedUser) {

        CreditCardExpensesOutput output = findCreditCardExpensesBoundary.execute(
                id,
                new FindCreditCardExpensesInput(parseYearMonth(effectiveMonth), name, page, size),
                authenticatedUser.ownerId()
        );

        return ResponseEntity.ok(mapper.toCreditCardExpensesResponseDto(output));
    }

    /**
     * Aggregated month view of everything that debits the credit card:
     * pure expenses (excluding standard-installment per-month children),
     * active installments, and recurring subscription previews. The legacy
     * {@code /expenses} endpoint above is preserved for callers that only
     * want pure Expense records.
     */
    @GetMapping("/{id}/charges")
    public ResponseEntity<CreditCardChargesResponseDto> findChargesByCreditCardId(
            @PathVariable String id,
            @RequestParam @Pattern(regexp = "^\\d{4}-\\d{2}$") String effectiveMonth,
            AuthenticatedUser authenticatedUser) {

        CreditCardChargesOutput output = findCreditCardChargesBoundary.execute(
                id,
                YearMonth.parse(effectiveMonth),
                authenticatedUser.ownerId()
        );
        return ResponseEntity.ok(mapper.toCreditCardChargesResponseDto(output));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CreditCardResponseDto> patch(@PathVariable String id,
                                                       @Valid @RequestBody CreditCardPatchRequestDto request,
                                                       AuthenticatedUser authenticatedUser) {
        CreditCardOutput output = patchCreditCardBoundary.execute(
                new PatchCreditCardInput(id, authenticatedUser.ownerId(), request.labels()));
        return ResponseEntity.ok(mapper.creditCardOutputToCreditCardResponseDto(output));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id,
                                           AuthenticatedUser authenticatedUser) {
        // Hard delete: credit cards cannot be removed while expenses/installments still reference them.
        deleteCreditCardByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }

    private YearMonth parseYearMonth(String value) {
        return value == null ? null : YearMonth.parse(value);
    }
}
