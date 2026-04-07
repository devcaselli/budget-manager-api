package br.com.casellisoftware.budgetmanager.rest.expense.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseResponseDto(
         String id,
         String name,
         BigDecimal cost,
         Instant purchaseDate,
         BigDecimal remaining,
         String walletId
) {
}
