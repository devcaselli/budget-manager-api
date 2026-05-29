package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

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
        String walletId,
        String creditCardId,
        Boolean installment,
        Integer installmentNumber,
        FlagEnum flag,
        String ownerId
) {
    public ExpenseInput(String name,
                        BigDecimal cost,
                        LocalDate purchaseDate,
                        String walletId,
                        String creditCardId,
                        Boolean installment,
                        Integer installmentNumber,
                        FlagEnum flag) {
        this(name, cost, purchaseDate, walletId, creditCardId, installment, installmentNumber, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public ExpenseInput {
        if (flag == null) {
            flag = FlagEnum.NONE;
        }
        if (ownerId == null || ownerId.isBlank()) {
            ownerId = AuthenticatedUser.LEGACY_OWNER_ID;
        }
    }

    public ExpenseInput withOwnerId(String ownerId) {
        return new ExpenseInput(name, cost, purchaseDate, walletId, creditCardId, installment, installmentNumber, flag, ownerId);
    }
}
