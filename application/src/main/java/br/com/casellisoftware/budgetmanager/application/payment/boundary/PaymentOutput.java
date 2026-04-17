package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.Instant;

public record PaymentOutput(
        String id,
        Money amount,
        Instant paymentDate,
        String details,
        String expenseId,
        String walletId,
        String bulletId
) {
}
