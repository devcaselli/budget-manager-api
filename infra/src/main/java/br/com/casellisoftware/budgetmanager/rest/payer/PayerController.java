package br.com.casellisoftware.budgetmanager.rest.payer;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.DeletePayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindAllPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindPayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PatchPayerBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.SavePayerBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerResponseDto;
import br.com.casellisoftware.budgetmanager.rest.payer.mappers.PayerRestMapper;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/payers")
@RequiredArgsConstructor
public class PayerController {

    private final SavePayerBoundary savePayerBoundary;
    private final PatchPayerBoundary patchPayerBoundary;
    private final FindPayerByIdBoundary findPayerByIdBoundary;
    private final FindAllPayersBoundary findAllPayersBoundary;
    private final DeletePayerByIdBoundary deletePayerByIdBoundary;
    private final PayerRestMapper mapper;

    @PostMapping
    public ResponseEntity<PayerResponseDto> save(@Valid @RequestBody PayerRequestDto request,
                                                 AuthenticatedUser authenticatedUser) {
        PayerOutput output = savePayerBoundary.execute(
                mapper.payerRequestDtoToPayerInput(request).withOwnerId(authenticatedUser.ownerId()));
        PayerResponseDto response = mapper.payerOutputToPayerResponseDto(output);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayerResponseDto> findById(@PathVariable String id,
                                                     AuthenticatedUser authenticatedUser) {
        PayerOutput output = findPayerByIdBoundary.findById(id, authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.payerOutputToPayerResponseDto(output));
    }

    /**
     * Global catalog of STANDING payers for the owner. TRANSIENT payers are
     * sub-resources of a wallet — query them via
     * {@code GET /wallets/{walletId}/payers}.
     */
    @GetMapping
    public ResponseEntity<List<PayerResponseDto>> findAll(AuthenticatedUser authenticatedUser) {
        List<PayerResponseDto> response = findAllPayersBoundary.execute(authenticatedUser.ownerId())
                .stream()
                .map(mapper::payerOutputToPayerResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PayerResponseDto> patch(@PathVariable String id,
                                                  @Valid @RequestBody PayerPatchRequestDto request,
                                                  AuthenticatedUser authenticatedUser) {
        PayerOutput output = patchPayerBoundary.execute(
                id,
                mapper.payerPatchRequestDtoToPayerPatchInput(request),
                authenticatedUser.ownerId());
        return ResponseEntity.ok(mapper.payerOutputToPayerResponseDto(output));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable String id,
                                           AuthenticatedUser authenticatedUser) {
        deletePayerByIdBoundary.execute(id, authenticatedUser.ownerId());
        return ResponseEntity.noContent().build();
    }
}
