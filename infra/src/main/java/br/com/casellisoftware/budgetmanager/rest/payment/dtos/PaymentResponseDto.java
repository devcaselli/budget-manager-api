package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentKind;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponseDto(
        String id,
        BigDecimal amount,
        String currency,
        Instant paymentDate,
        String details,
        String expenseId,
        String walletId,
        String bulletId,
        FlagEnum flag,
        PaymentKind kind,
        String payerId,
        String shareId,
        boolean reversal,
        String reversedPaymentId
) {
    public PaymentResponseDto(String id,
                              BigDecimal amount,
                              String currency,
                              Instant paymentDate,
                              String details,
                              String expenseId,
                              String walletId,
                              String bulletId) {
        this(id, amount, currency, paymentDate, details, expenseId, walletId, bulletId, FlagEnum.NONE, PaymentKind.NORMAL, null, null, false, null);
    }

    public PaymentResponseDto(String id,
                              BigDecimal amount,
                              String currency,
                              Instant paymentDate,
                              String details,
                              String expenseId,
                              String walletId,
                              String bulletId,
                              FlagEnum flag) {
        this(id, amount, currency, paymentDate, details, expenseId, walletId, bulletId, flag, PaymentKind.NORMAL, null, null, false, null);
    }
}
