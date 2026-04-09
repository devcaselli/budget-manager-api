package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input DTO for the save-expense use case. Carries raw primitive types from the
 * interface adapter layer; canonical validation happens when the use case calls
 * {@code Expense.create(...)}.
 */
public record ExpenseInput(
        String name,
        BigDecimal cost,
        LocalDate purchaseDate,
        String walletId
) {
}
