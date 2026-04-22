package br.com.casellisoftware.budgetmanager.application.expense.boundary;

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
        LocalDate purchaseDate
) {
}
