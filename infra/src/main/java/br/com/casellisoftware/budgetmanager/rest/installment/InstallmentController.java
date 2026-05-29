package br.com.casellisoftware.budgetmanager.rest.installment;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.DeleteInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentWalletFilter;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentInput;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentSortOrder;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.PagedInstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.SaveStandaloneInstallmentRequestDto;
import br.com.casellisoftware.budgetmanager.rest.installment.mappers.InstallmentRestMapper;
import jakarta.validation.Valid;
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
import java.util.List;

@Validated
@RestController
@RequestMapping("/installments")
@RequiredArgsConstructor
public class InstallmentController {

    private final FindInstallmentByIdBoundary findInstallmentByIdBoundary;
    private final FindInstallmentsByWalletIdBoundary findInstallmentsByWalletIdBoundary;
    private final DeleteInstallmentBoundary deleteInstallmentBoundary;
    private final SaveStandaloneInstallmentBoundary saveStandaloneInstallmentBoundary;
    private final PatchInstallmentBoundary patchInstallmentBoundary;
    private final InstallmentRestMapper mapper;

    @PostMapping
    public ResponseEntity<InstallmentResponseDto> save(
            @Valid @RequestBody SaveStandaloneInstallmentRequestDto request,
            AuthenticatedUser authenticatedUser) {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                request.description(),
                request.details(),
                request.originalValue(),
                request.installmentValue(),
                request.currency(),
                request.installmentNumber(),
                request.purchaseDate(),
                request.creditCardId(),
                request.sourceEffectiveMonth(),
                request.flag(),
                authenticatedUser.ownerId()
        );
        InstallmentOutput output = saveStandaloneInstallmentBoundary.execute(input);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(output.id())
                .toUri();
        return ResponseEntity.created(location).body(mapper.installmentOutputToInstallmentResponseDto(output));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstallmentResponseDto> findById(@PathVariable String id,
                                                           AuthenticatedUser authenticatedUser) {
        InstallmentOutput output = findInstallmentByIdBoundary.findById(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.installmentOutputToInstallmentResponseDto(output));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<PagedInstallmentResponseDto> findByWalletId(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String creditCardId,
            @RequestParam(required = false, defaultValue = "ENDING_SOON") InstallmentSortOrder sort,
            AuthenticatedUser authenticatedUser) {
        InstallmentWalletFilter filter = new InstallmentWalletFilter(page, size, creditCardId, sort);
        PageResult<InstallmentOutput> result = findInstallmentsByWalletIdBoundary.execute(walletId, filter, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toPagedResponse(result));
    }

    @GetMapping("/wallet/{walletId}/all")
    public ResponseEntity<List<InstallmentResponseDto>> findAllByWalletId(
            @PathVariable String walletId,
            @RequestParam(required = false) String creditCardId,
            @RequestParam(required = false, defaultValue = "ENDING_SOON") InstallmentSortOrder sort,
            AuthenticatedUser authenticatedUser) {
        List<InstallmentResponseDto> response = findInstallmentsByWalletIdBoundary
                .executeAll(walletId, creditCardId, sort, authenticatedUser.ownerId())
                .stream()
                .map(mapper::installmentOutputToInstallmentResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<InstallmentResponseDto> patch(@PathVariable String id,
                                                        @Valid @RequestBody InstallmentPatchRequestDto request,
                                                        AuthenticatedUser authenticatedUser) {
        var input = mapper.toPatchInput(id, request).withOwnerId(authenticatedUser.ownerId());
        InstallmentOutput output = patchInstallmentBoundary.execute(input);
        return ResponseEntity.ok(mapper.installmentOutputToInstallmentResponseDto(output));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id,
                                           AuthenticatedUser authenticatedUser) {
        // Logical delete: installments are marked deleted and the linked per-installment expense is cascaded.
        deleteInstallmentBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }
}
