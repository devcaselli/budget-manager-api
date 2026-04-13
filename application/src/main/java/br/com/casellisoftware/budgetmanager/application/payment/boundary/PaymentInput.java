package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import java.math.BigDecimal;

public record PaymentInput (
        BigDecimal amount,
        String details,
        String expenseId,
        String walletId,
        String bulletId
) {
}
