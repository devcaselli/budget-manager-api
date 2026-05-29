package br.com.casellisoftware.budgetmanager.rest.payer.dtos;

import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PayerResponseDto(
        String id,
        String name,
        PayerType type,
        String walletId,
        String subscriptionId,
        LocalDate paymentDate,
        BigDecimal amountDue,
        BigDecimal monthlyAmount,
        BigDecimal journeyAmount,
        String currency,
        boolean deleted
) {
}
