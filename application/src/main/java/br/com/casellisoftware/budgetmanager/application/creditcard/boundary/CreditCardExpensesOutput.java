package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.math.BigDecimal;

public record CreditCardExpensesOutput(
        PageResult<ExpenseOutput> expenses,
        BigDecimal totalCost
) {
}
