package br.com.casellisoftware.budgetmanager.rest.payment;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentInput;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import br.com.casellisoftware.budgetmanager.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<Void> pay(@RequestBody PayRequestDto request,@RequestParam(value = "walletId") String walletId) {

        this.paymentService.pay(request, walletId);

        return ResponseEntity.noContent().build();
    }
}
