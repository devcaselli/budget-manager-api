package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record ExpenseInput(
        String name,
        BigDecimal cost,
        Instant purchaseDate,
        String walletId
) {
    public ExpenseInput {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(cost, "cost must not be null");
        if (cost.signum() <= 0) {
            throw new IllegalArgumentException("cost must be positive");
        }
        Objects.requireNonNull(walletId, "walletId must not be null");
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
    }
}
