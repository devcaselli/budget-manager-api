package br.com.casellisoftware.budgetmanager.rest.expense.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseRequestDto(
         String name,
         BigDecimal cost,
         Instant purchaseDate,
         String walletId
) {
}
