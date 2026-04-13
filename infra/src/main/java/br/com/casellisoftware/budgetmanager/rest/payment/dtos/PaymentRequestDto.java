package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentRequestDto (
        BigDecimal amount,
        Instant paymentDate,
        String details
) {
}
