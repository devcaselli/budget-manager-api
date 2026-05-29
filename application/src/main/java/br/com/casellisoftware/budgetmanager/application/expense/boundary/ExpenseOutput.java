package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseOutput(
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
        String installmentId,
        List<String> paymentIds,
        FlagEnum flag
) {
    public ExpenseOutput(String id,
                         String name,
                         BigDecimal cost,
                         LocalDate purchaseDate,
                         String walletId,
                         String creditCardId,
                         BigDecimal remaining,
                         List<String> paymentIds,
                         FlagEnum flag) {
        this(id, name, cost, purchaseDate, remaining, walletId, null, creditCardId, false, null, null, paymentIds, flag);
    }

    public ExpenseOutput(String id,
                         String name,
                         BigDecimal cost,
                         LocalDate purchaseDate,
                         BigDecimal remaining,
                         String walletId,
                         String bulletId,
                         String creditCardId,
                         boolean installment,
                         Integer installmentNumber,
                         List<String> paymentIds,
                         FlagEnum flag) {
        this(id, name, cost, purchaseDate, remaining, walletId, bulletId, creditCardId, installment, installmentNumber, null, paymentIds, flag);
    }
}
