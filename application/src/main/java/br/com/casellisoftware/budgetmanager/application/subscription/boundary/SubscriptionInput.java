package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Input DTO for the save-subscription use case. {@code state} is already
 * resolved to the domain enum by the interface adapter layer before reaching
 * the use case; canonical domain validation happens when the use case calls
 * {@code Subscription.create(...)}.
 */
public record SubscriptionInput(
        String description,
        BigDecimal amount,
        String currency,
        YearMonth effectiveMonth,
        SubscriptionState state,
        FlagEnum flag,
        String ownerId,
        String creditCardId
) {
    public SubscriptionInput(String description, BigDecimal amount, String currency, YearMonth effectiveMonth, SubscriptionState state, FlagEnum flag, String creditCardId) {
        this(description, amount, currency, effectiveMonth, state, flag, AuthenticatedUser.LEGACY_OWNER_ID, creditCardId);
    }

    public SubscriptionInput withOwnerId(String ownerId) {
        return new SubscriptionInput(description, amount, currency, effectiveMonth, state, flag, ownerId, creditCardId);
    }
}
