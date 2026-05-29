package br.com.casellisoftware.budgetmanager.rest.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.FindSubscriptionChargesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos.SubscriptionChargeResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.mappers.SubscriptionChargeRestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wallets/{walletId}/subscription-charges")
@RequiredArgsConstructor
public class SubscriptionChargeController {

    private final FindSubscriptionChargesByWalletIdBoundary findSubscriptionChargesByWalletIdBoundary;
    private final SubscriptionChargeRestMapper mapper;

    @GetMapping
    public ResponseEntity<List<SubscriptionChargeResponseDto>> findByWalletId(@PathVariable String walletId,
                                                                              AuthenticatedUser authenticatedUser) {
        List<SubscriptionChargeResponseDto> response = findSubscriptionChargesByWalletIdBoundary.execute(walletId, authenticatedUser.ownerId())
                .stream()
                .map(mapper::toResponse)
                .toList();

        return ResponseEntity.ok(response);
    }
}
