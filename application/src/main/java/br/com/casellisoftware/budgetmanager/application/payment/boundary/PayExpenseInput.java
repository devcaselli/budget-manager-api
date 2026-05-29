package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.Instant;

/**
 * Input DTO for the pay-expense use case.
 *
 * <p>Carries everything needed to register a payment against an expense
 * and decrement its matching bullet. The REST layer is responsible for
 * validating each field and composing the {@link Money} value object from
 * the raw {@code amount} + {@code currency} HTTP parameters — this record
 * is a pure transport contract between the HTTP adapter and the application layer.</p>
 */
public record PayExpenseInput(
        Money amount,
        Instant paymentDate,
        String details,
        String expenseId,
        String walletId,
        String bulletId,
        FlagEnum flag,
        String ownerId
) {
    public PayExpenseInput(Money amount,
                           Instant paymentDate,
                           String details,
                           String expenseId,
                           String walletId,
                           String bulletId,
                           FlagEnum flag) {
        this(amount, paymentDate, details, expenseId, walletId, bulletId, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PayExpenseInput(Money amount,
                           Instant paymentDate,
                           String details,
                           String expenseId,
                           String walletId,
                           String bulletId) {
        this(amount, paymentDate, details, expenseId, walletId, bulletId, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PayExpenseInput withOwnerId(String ownerId) {
        return new PayExpenseInput(amount, paymentDate, details, expenseId, walletId, bulletId, flag, ownerId);
    }
}
