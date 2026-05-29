package br.com.casellisoftware.budgetmanager.rest.wallet;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindWalletPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerResponseDto;
import br.com.casellisoftware.budgetmanager.rest.payer.mappers.PayerRestMapper;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.mappers.WalletRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final SaveWalletBoundary saveWalletBoundary;
    private final FindAllWalletsBoundary findAllWalletsBoundary;
    private final FindWalletByIdBoundary findWalletByIdBoundary;
    private final PatchWalletBoundary patchWalletBoundary;
    private final FindWalletPayersBoundary findWalletPayersBoundary;
    private final WalletRestMapper walletRestMapper;
    private final PayerRestMapper payerRestMapper;

    @PostMapping
    public ResponseEntity<WalletResponseDto> save(@Valid @RequestBody WalletRequestDto walletRequestDto,
                                                  AuthenticatedUser authenticatedUser) {
        WalletInput input = walletRestMapper.walletRequestDtoToWalletInput(walletRequestDto)
                .withOwnerId(authenticatedUser.ownerId());
        WalletResponseDto response = walletRestMapper.walletOutputToWalletResponseDto(this.saveWalletBoundary.execute(input));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<WalletResponseDto>> findAll(AuthenticatedUser authenticatedUser) {
        List<WalletResponseDto> response = findAllWalletsBoundary.execute(authenticatedUser.ownerId())
                .stream()
                .map(walletRestMapper::walletOutputToWalletResponseDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponseDto> findById(@PathVariable String id,
                                                      AuthenticatedUser authenticatedUser) {
        WalletOutput output = findWalletByIdBoundary.findById(id, authenticatedUser.ownerId());
        WalletResponseDto response = walletRestMapper.walletOutputToWalletResponseDto(output);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WalletResponseDto> patch(@PathVariable String id,
                                                   @Valid @RequestBody WalletPatchRequestDto body,
                                                   AuthenticatedUser authenticatedUser) {
        PatchWalletInput input = new PatchWalletInput(
                id,
                body.description(),
                body.budget(),
                null,
                body.closedDate(),
                body.closed(),
                body.state(),
                body.flag(),
                authenticatedUser.ownerId()
        );
        WalletOutput output = patchWalletBoundary.execute(input);
        return ResponseEntity.ok(walletRestMapper.walletOutputToWalletResponseDto(output));
    }

    @GetMapping("/{id}/payers")
    public ResponseEntity<List<PayerResponseDto>> findWalletPayers(@PathVariable("id") String walletId,
                                                                   AuthenticatedUser authenticatedUser) {
        List<PayerResponseDto> response = findWalletPayersBoundary.execute(walletId, authenticatedUser.ownerId())
                .stream()
                .map(payerRestMapper::payerOutputToPayerResponseDto)
                .toList();
        return ResponseEntity.ok(response);
    }
}
