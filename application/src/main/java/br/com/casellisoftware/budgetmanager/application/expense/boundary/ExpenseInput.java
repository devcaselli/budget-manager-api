package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseInput(
        String name,
        BigDecimal cost,
        Instant purchaseDate,
        String walletId
) {

}
