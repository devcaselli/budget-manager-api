package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Input DTO for the patch-subscription use case. All fields except {@code id}
 * are nullable — only non-null fields will be applied to the existing entity.
 *
 * <p>{@code effectiveMonth} anchors an amount change to a specific month (typically
 * the effectiveMonth of the wallet the user is editing from). When null, the use case
 * falls back to the current clock month.</p>
 */
public record PatchSubscriptionInput(
        String id,
        String description,
        BigDecimal newAmount,
        String creditCardId,
        FlagEnum flag,
        String ownerId,
        YearMonth effectiveMonth
) {
    public PatchSubscriptionInput(String id, String description, BigDecimal newAmount, String creditCardId, FlagEnum flag) {
        this(id, description, newAmount, creditCardId, flag, AuthenticatedUser.LEGACY_OWNER_ID, null);
    }

    public PatchSubscriptionInput(String id, String description, BigDecimal newAmount, FlagEnum flag) {
        this(id, description, newAmount, null, flag, AuthenticatedUser.LEGACY_OWNER_ID, null);
    }

    public PatchSubscriptionInput(String id, String description, BigDecimal newAmount, String creditCardId) {
        this(id, description, newAmount, creditCardId, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID, null);
    }

    public PatchSubscriptionInput(String id, String description, BigDecimal newAmount) {
        this(id, description, newAmount, null, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID, null);
    }

    public PatchSubscriptionInput withOwnerId(String ownerId) {
        return new PatchSubscriptionInput(id, description, newAmount, creditCardId, flag, ownerId, effectiveMonth);
    }
}
