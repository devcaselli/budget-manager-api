package br.com.casellisoftware.budgetmanager.rest.reservedbudget;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.DeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindActiveReservedBudgetsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindAllReservedBudgetsBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindReservedBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.SaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.PagedReservedBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetRequestDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.mappers.ReservedBudgetRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;

@Validated
@RestController
@RequestMapping("/reserved-budgets")
@RequiredArgsConstructor
public class ReservedBudgetController {

    private final SaveReservedBudgetBoundary saveReservedBudgetBoundary;
    private final PatchReservedBudgetBoundary patchReservedBudgetBoundary;
    private final DeleteReservedBudgetBoundary deleteReservedBudgetBoundary;
    private final FindReservedBudgetByIdBoundary findReservedBudgetByIdBoundary;
    private final FindAllReservedBudgetsBoundary findAllReservedBudgetsBoundary;
    private final FindActiveReservedBudgetsByMonthBoundary findActiveReservedBudgetsByMonthBoundary;
    private final ReservedBudgetRestMapper mapper;

    @PostMapping
    public ResponseEntity<ReservedBudgetResponseDto> save(@Valid @RequestBody ReservedBudgetRequestDto request,
                                                          AuthenticatedUser authenticatedUser) {
        ReservedBudgetOutput output = saveReservedBudgetBoundary
                .execute(mapper.toInput(request).withOwnerId(authenticatedUser.ownerId()));
        ReservedBudgetResponseDto response = mapper.toResponse(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReservedBudgetResponseDto> patch(@PathVariable String id,
                                                           @Valid @RequestBody ReservedBudgetPatchRequestDto request,
                                                           AuthenticatedUser authenticatedUser) {
        ReservedBudgetOutput output = patchReservedBudgetBoundary
                .execute(mapper.toPatchInput(id, request).withOwnerId(authenticatedUser.ownerId()));
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       AuthenticatedUser authenticatedUser) {
        deleteReservedBudgetBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservedBudgetResponseDto> findById(@PathVariable String id,
                                                              AuthenticatedUser authenticatedUser) {
        ReservedBudgetOutput output = findReservedBudgetByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @GetMapping
    public ResponseEntity<PagedReservedBudgetResponseDto> findAll(
            @RequestParam(required = false)
            @Pattern(regexp = "\\d{4}-\\d{2}", message = "activeAt must use YYYY-MM format")
            String activeAt,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            AuthenticatedUser authenticatedUser) {

        if (activeAt != null) {
            List<ReservedBudgetOutput> activeReservedBudgets = findActiveReservedBudgetsByMonthBoundary
                    .execute(YearMonth.parse(activeAt), authenticatedUser.ownerId());
            return ResponseEntity.ok(mapper.toPagedResponse(activeReservedBudgets));
        }

        PageResult<ReservedBudgetOutput> reservedBudgets = findAllReservedBudgetsBoundary
                .execute(page, size, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toPagedResponse(reservedBudgets));
    }
}
