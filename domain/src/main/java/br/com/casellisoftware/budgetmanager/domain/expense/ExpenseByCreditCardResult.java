package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.math.BigDecimal;
import java.util.Objects;

public record ExpenseByCreditCardResult(
        PageResult<Expense> expenses,
        BigDecimal totalCost
) {

    public ExpenseByCreditCardResult {
        Objects.requireNonNull(expenses, "expenses must not be null");
        Objects.requireNonNull(totalCost, "totalCost must not be null");
    }
}
