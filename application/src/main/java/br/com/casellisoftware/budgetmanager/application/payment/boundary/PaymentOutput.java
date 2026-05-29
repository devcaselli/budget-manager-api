package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentKind;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.Instant;

public record PaymentOutput(
        String id,
        Money amount,
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
    public PaymentOutput(String id, Money amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId) {
        this(id, amount, paymentDate, details, expenseId, walletId, bulletId, FlagEnum.NONE, PaymentKind.NORMAL, null, null, false, null);
    }

    public PaymentOutput(String id, Money amount, Instant paymentDate, String details, String expenseId, String walletId, String bulletId, FlagEnum flag) {
        this(id, amount, paymentDate, details, expenseId, walletId, bulletId, flag, PaymentKind.NORMAL, null, null, false, null);
    }
}
