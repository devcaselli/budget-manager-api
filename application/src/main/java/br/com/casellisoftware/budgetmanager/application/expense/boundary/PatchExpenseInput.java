package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input DTO for the patch-expense use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 * Financial fields such as remaining balance and payment ids are intentionally
 * absent because they mutate only through the payment flow.
 */
public record PatchExpenseInput(
        String id,
        String name,
        BigDecimal cost,
        LocalDate purchaseDate,
        FlagEnum flag,
        String ownerId
) {
    public PatchExpenseInput(String id, String name, BigDecimal cost, LocalDate purchaseDate, FlagEnum flag) {
        this(id, name, cost, purchaseDate, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchExpenseInput(String id, String name, BigDecimal cost, LocalDate purchaseDate) {
        this(id, name, cost, purchaseDate, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchExpenseInput withOwnerId(String ownerId) {
        return new PatchExpenseInput(id, name, cost, purchaseDate, flag, ownerId);
    }
}
