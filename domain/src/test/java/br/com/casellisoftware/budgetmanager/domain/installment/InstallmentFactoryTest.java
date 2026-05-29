package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentFactoryTest {

    @Test
    void fromExpense_specExample_buildsExpectedInstallment() {
        Expense expense = new Expense(
                "exp-1",
                "w1",
                "cc1",
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("6000.00")),
                LocalDate.of(2026, 5, 10),
                List.of(),
                FlagEnum.NONE
        );

        Installment installment = InstallmentFactory.fromExpense(
                expense, 6, YearMonth.of(2026, 5), FlagEnum.NONE);

        assertThat(installment.getDescription()).isEqualTo("Notebook");
        assertThat(installment.getOriginalValue().amount()).isEqualByComparingTo("6000.00");
        assertThat(installment.getInstallmentValue().amount()).isEqualByComparingTo("1000.00");
        assertThat(installment.getInstallmentNumber()).isEqualTo(6);
        assertThat(installment.getCreditCardId()).isEqualTo("cc1");
        assertThat(installment.getSourceWalletId()).isEqualTo("w1");
        assertThat(installment.getSourceEffectiveMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(installment.getLastInstallmentDate()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(installment.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    void fromExpense_residualSplit_passesToleranceCheck() {
        Expense expense = new Expense(
                "exp-2",
                "w1",
                "cc1",
                "Lunch",
                Money.of(new BigDecimal("100.00")),
                Money.of(new BigDecimal("100.00")),
                LocalDate.of(2026, 5, 10),
                List.of(),
                FlagEnum.NONE
        );

        Installment installment = InstallmentFactory.fromExpense(
                expense, 3, YearMonth.of(2026, 5), FlagEnum.NONE);

        assertThat(installment.getInstallmentValue().amount()).isEqualByComparingTo("33.33");
    }
}
