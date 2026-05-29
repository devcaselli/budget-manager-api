package br.com.casellisoftware.budgetmanager.rest.extrabudget;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.DeleteExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByBulletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.SaveExtraBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.ExtraBudgetRequestDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.ExtraBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.mappers.ExtraBudgetRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Single REST controller for all extra-budget operations. Each method delegates to
 * the corresponding application-layer use case and uses {@link ExtraBudgetRestMapper}
 * to translate between HTTP DTOs and application boundary records.
 */
@RestController
@RequestMapping("/extra-budgets")
@RequiredArgsConstructor
public class ExtraBudgetController {

    private final SaveExtraBudgetBoundary saveExtraBudgetBoundary;
    private final FindExtraBudgetByIdBoundary findExtraBudgetByIdBoundary;
    private final FindExtraBudgetsByWalletIdBoundary findExtraBudgetsByWalletIdBoundary;
    private final FindExtraBudgetsByBulletIdBoundary findExtraBudgetsByBulletIdBoundary;
    private final DeleteExtraBudgetByIdBoundary deleteExtraBudgetByIdBoundary;
    private final ExtraBudgetRestMapper mapper;

    @PostMapping
    public ResponseEntity<ExtraBudgetResponseDto> save(@Valid @RequestBody ExtraBudgetRequestDto request,
                                                       AuthenticatedUser authenticatedUser) {
        ExtraBudgetOutput output = saveExtraBudgetBoundary.execute(
                mapper.toInput(request).withOwnerId(authenticatedUser.ownerId())
        );

        ExtraBudgetResponseDto response = mapper.toResponse(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExtraBudgetResponseDto> findById(@PathVariable String id,
                                                           AuthenticatedUser authenticatedUser) {
        ExtraBudgetOutput output = findExtraBudgetByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.toResponse(output));
    }

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<List<ExtraBudgetResponseDto>> findByWalletId(@PathVariable String walletId,
                                                                        AuthenticatedUser authenticatedUser) {
        List<ExtraBudgetResponseDto> response = findExtraBudgetsByWalletIdBoundary
                .execute(walletId, authenticatedUser.ownerId())
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/bullet/{bulletId}")
    public ResponseEntity<List<ExtraBudgetResponseDto>> findByBulletId(@PathVariable String bulletId,
                                                                        AuthenticatedUser authenticatedUser) {
        List<ExtraBudgetResponseDto> response = findExtraBudgetsByBulletIdBoundary
                .execute(bulletId, authenticatedUser.ownerId())
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id,
                                       AuthenticatedUser authenticatedUser) {
        deleteExtraBudgetByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }
}
