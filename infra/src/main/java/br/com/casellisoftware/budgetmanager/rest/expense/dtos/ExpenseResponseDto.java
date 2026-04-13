package br.com.casellisoftware.budgetmanager.rest.expense.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseResponseDto(
         String id,
         String name,
         BigDecimal cost,
         LocalDate purchaseDate,
         BigDecimal remaining,
         String walletId,
         List<String> paymentIds
) {
}
