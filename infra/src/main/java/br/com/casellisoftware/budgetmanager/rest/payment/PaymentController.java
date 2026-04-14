package br.com.casellisoftware.budgetmanager.rest.payment;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payment.mappers.PaymentRestMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
@RequiredArgsConstructor
public class PaymentController {

    private final PayExpenseBoundary payExpenseBoundary;
    private final PaymentRestMapper mapper;

    @PostMapping
    public ResponseEntity<Void> pay(@RequestBody PayRequestDto request,
                                    @RequestParam("walletId") String walletId) {
        payExpenseBoundary.execute(mapper.toPayExpenseInput(request, walletId));
        return ResponseEntity.noContent().build();
    }
}
