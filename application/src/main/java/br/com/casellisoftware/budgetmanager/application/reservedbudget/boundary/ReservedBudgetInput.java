package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Input DTO for the save-reserved-budget use case. Canonical domain validation happens
 * when the use case calls {@code ReservedBudget.create(...)}.
 */
public record ReservedBudgetInput(
        String description,
        String details,
        BigDecimal budget,
        String currency,
        YearMonth effectiveMonth,
        FlagEnum flag,
        String ownerId
) {
    public ReservedBudgetInput(String description, String details, BigDecimal budget, String currency, YearMonth effectiveMonth, FlagEnum flag) {
        this(description, details, budget, currency, effectiveMonth, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public ReservedBudgetInput withOwnerId(String ownerId) {
        return new ReservedBudgetInput(description, details, budget, currency, effectiveMonth, flag, ownerId);
    }
}
