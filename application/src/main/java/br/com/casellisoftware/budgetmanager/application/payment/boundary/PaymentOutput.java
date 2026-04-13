package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentOutput(
        String id,
        BigDecimal amount,
        Instant paymentDate,
        String details,
        String expenseId,
        String walletId
) {
}
