package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseInputTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 7);

    @Test
    void validInput_isConstructed() {
        ExpenseInput input = new ExpenseInput("lunch", new BigDecimal("10.00"), DATE, "wallet-1");

        assertThat(input.name()).isEqualTo("lunch");
        assertThat(input.cost()).isEqualByComparingTo("10.00");
        assertThat(input.purchaseDate()).isEqualTo(DATE);
        assertThat(input.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void acceptsNullFields_validationHappensInDomain() {
        // ExpenseInput is a transport-level DTO. Canonical validation lives in
        // Expense.create(...), which the use case invokes.
        ExpenseInput input = new ExpenseInput(null, null, null, null);
        assertThat(input.name()).isNull();
        assertThat(input.cost()).isNull();
        assertThat(input.purchaseDate()).isNull();
        assertThat(input.walletId()).isNull();
    }
}
