package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Input DTO for the patch-expense use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 */
public record PatchExpenseInput(
        String id,
        String name,
        BigDecimal cost,
        BigDecimal remaining,
        LocalDate purchaseDate,
        List<String> paymentIds
) {
}
