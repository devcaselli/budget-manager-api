package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;

public record PaymentInput (
        BigDecimal amount,
        String details,
        String expenseId,
        String walletId,
        String bulletId,
        FlagEnum flag,
        String ownerId
) {
    public PaymentInput(BigDecimal amount, String details, String expenseId, String walletId, String bulletId, FlagEnum flag) {
        this(amount, details, expenseId, walletId, bulletId, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PaymentInput(BigDecimal amount, String details, String expenseId, String walletId, String bulletId) {
        this(amount, details, expenseId, walletId, bulletId, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }
}
