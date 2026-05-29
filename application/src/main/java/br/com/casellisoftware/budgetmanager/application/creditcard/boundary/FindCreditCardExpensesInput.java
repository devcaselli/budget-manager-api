package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import java.time.YearMonth;

public record FindCreditCardExpensesInput(
        YearMonth effectiveMonth,
        String name,
        int page,
        int size
) {

    public FindCreditCardExpensesInput {
        name = name == null || name.isBlank() ? null : name.trim();
    }
}
