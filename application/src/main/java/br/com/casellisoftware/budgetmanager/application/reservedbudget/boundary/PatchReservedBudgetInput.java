package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Input DTO for the patch-reserved-budget use case. All fields except {@code id} are
 * nullable — only non-null fields are applied. A non-null {@code newAmount} is materialized
 * as a new version effective from {@code effectiveMonth} (or the current month when that is
 * null), leaving earlier months untouched.
 */
public record PatchReservedBudgetInput(
        String id,
        String description,
        String details,
        BigDecimal newAmount,
        FlagEnum flag,
        YearMonth effectiveMonth,
        String ownerId
) {
    public PatchReservedBudgetInput(String id, String description, String details, BigDecimal newAmount, FlagEnum flag) {
        this(id, description, details, newAmount, flag, null, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchReservedBudgetInput(String id, String description, String details, BigDecimal newAmount, FlagEnum flag, YearMonth effectiveMonth) {
        this(id, description, details, newAmount, flag, effectiveMonth, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchReservedBudgetInput withOwnerId(String ownerId) {
        return new PatchReservedBudgetInput(id, description, details, newAmount, flag, effectiveMonth, ownerId);
    }
}
