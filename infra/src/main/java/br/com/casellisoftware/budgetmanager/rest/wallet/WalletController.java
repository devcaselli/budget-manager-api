package br.com.casellisoftware.budgetmanager.rest.wallet;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.mappers.WalletRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final SaveWalletBoundary saveWalletBoundary;
    private final WalletRestMapper  walletRestMapper;

    @PostMapping
    public ResponseEntity<WalletResponseDto> save(@Valid @RequestBody WalletRequestDto walletRequestDto) {
        WalletInput input = walletRestMapper.walletRequestDtoToWalletInput(walletRequestDto);
        WalletResponseDto response = walletRestMapper.walletOutputToWalletResponseDto(this.saveWalletBoundary.execute(input));

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }
}
