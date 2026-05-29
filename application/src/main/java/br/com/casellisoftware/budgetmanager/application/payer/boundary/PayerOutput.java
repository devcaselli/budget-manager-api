package br.com.casellisoftware.budgetmanager.application.payer.boundary;

import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;

public record PayerOutput(
        String id,
        String name,
        PayerType type,
        String walletId,
        String subscriptionId,
        LocalDate paymentDate,
        Money amountDue,
        Money monthlyAmount,
        Money journeyAmount,
        boolean deleted
) {
}
