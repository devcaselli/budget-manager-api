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
         String bulletId,
         String creditCardId,
         boolean installment,
         Integer installmentNumber,
         String installmentId
) {
    public ExpenseResponseDto(String id,
                              String name,
                              BigDecimal cost,
                              LocalDate purchaseDate,
                              BigDecimal remaining,
                              String walletId,
                              String creditCardId,
                              List<String> paymentIds,
                              br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum flag) {
        this(id, name, cost, purchaseDate, remaining, walletId, null, creditCardId, false, null, null);
    }
}
