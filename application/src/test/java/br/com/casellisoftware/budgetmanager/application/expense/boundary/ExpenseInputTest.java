package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseInputTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 7);

    @Test
    void validInput_isConstructed() {
        ExpenseInput input = new ExpenseInput(
                "lunch",
                new BigDecimal("10.00"),
                DATE,
                "wallet-1",
                "cc-1",
                false,
                null,
                FlagEnum.NONE
        );

        assertThat(input.name()).isEqualTo("lunch");
        assertThat(input.cost()).isEqualByComparingTo("10.00");
        assertThat(input.purchaseDate()).isEqualTo(DATE);
        assertThat(input.walletId()).isEqualTo("wallet-1");
        assertThat(input.creditCardId()).isEqualTo("cc-1");
        assertThat(input.flag()).isEqualTo(FlagEnum.NONE);
    }

    @Test
    void acceptsNullFields_validationHappensInDomain() {
        ExpenseInput input = new ExpenseInput(null, null, null, null, null, false, null, null);
        assertThat(input.name()).isNull();
        assertThat(input.cost()).isNull();
        assertThat(input.purchaseDate()).isNull();
        assertThat(input.walletId()).isNull();
    }

    @Test
    void installmentWithoutNumber_isTransportOnlyAndDoesNotThrow() {
        ExpenseInput input = new ExpenseInput(
                "notebook",
                new BigDecimal("1000.00"),
                DATE,
                "wallet-1",
                "cc-1",
                true,
                null,
                FlagEnum.NONE
        );

        assertThat(input.installment()).isTrue();
        assertThat(input.installmentNumber()).isNull();
    }
}
