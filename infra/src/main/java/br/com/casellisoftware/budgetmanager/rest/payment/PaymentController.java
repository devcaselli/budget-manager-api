package br.com.casellisoftware.budgetmanager.rest.payment;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payment.mappers.PaymentRestMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PayExpenseBoundary payExpenseBoundary;
    private final PaymentRestMapper mapper;

    @PostMapping
    public ResponseEntity<Void> pay(@Valid @RequestBody PayRequestDto request,
                                    @RequestParam("walletId") @NotBlank String walletId) {
        final PaymentOutput output = payExpenseBoundary.execute(mapper.toPayExpenseInput(request, walletId));
        final var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/payments/{id}")
                .buildAndExpand(output.id())
                .toUri();
        return ResponseEntity.created(location).build();
    }
}
