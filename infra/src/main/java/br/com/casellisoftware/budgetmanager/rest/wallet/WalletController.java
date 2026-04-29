package br.com.casellisoftware.budgetmanager.rest.wallet;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.mappers.WalletRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final WalletRestMapper walletRestMapper;

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

    @GetMapping
    public ResponseEntity<List<WalletResponseDto>> findAll() {
        List<WalletResponseDto> response = findAllWalletsBoundary.execute()
                .stream()
                .map(walletRestMapper::walletOutputToWalletResponseDto)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WalletResponseDto> findById(@PathVariable String id) {
        WalletOutput output = findWalletByIdBoundary.findById(id);
        WalletResponseDto response = walletRestMapper.walletOutputToWalletResponseDto(output);

        return ResponseEntity.ok(response);
    }
}
