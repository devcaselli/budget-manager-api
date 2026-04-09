package br.com.casellisoftware.budgetmanager.rest.expense.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequestDto(
         String name,
         BigDecimal cost,
         LocalDate purchaseDate,
         String walletId
) {
}
