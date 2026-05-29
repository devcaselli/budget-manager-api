package br.com.casellisoftware.budgetmanager.rest.payment;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindPaymentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PagedPaymentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PaymentResponseDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final FindPaymentsByWalletIdBoundary findPaymentsByWalletIdBoundary;

    @GetMapping("/wallet/{walletId}")
    public ResponseEntity<PagedPaymentResponseDto> findByWalletId(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            AuthenticatedUser authenticatedUser) {

        PageResult<PaymentOutput> result = findPaymentsByWalletIdBoundary.execute(walletId, page, size, authenticatedUser.ownerId());
        List<PaymentResponseDto> content = result.content()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new PagedPaymentResponseDto(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        ));
    }

    private PaymentResponseDto toResponse(PaymentOutput output) {
        return new PaymentResponseDto(
                output.id(),
                output.amount().amount(),
                output.amount().currency().getCurrencyCode(),
                output.paymentDate(),
                output.details(),
                output.expenseId(),
                output.walletId(),
                output.bulletId(),
                output.flag(),
                output.kind(),
                output.payerId(),
                output.shareId(),
                output.reversal(),
                output.reversedPaymentId()
        );
    }
}
