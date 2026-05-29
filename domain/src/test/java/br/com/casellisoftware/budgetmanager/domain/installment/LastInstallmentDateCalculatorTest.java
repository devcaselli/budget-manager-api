package br.com.casellisoftware.budgetmanager.domain.installment;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LastInstallmentDateCalculatorTest {

    @Test
    void calculate_specExample_2026_05_with_6_returns_2026_10() {
        YearMonth result = LastInstallmentDateCalculator.calculate(
                LocalDate.of(2026, 5, 10), 6);

        assertThat(result).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void calculate_twoInstallments_returnsNextMonth() {
        YearMonth result = LastInstallmentDateCalculator.calculate(
                YearMonth.of(2026, 1), 2);

        assertThat(result).isEqualTo(YearMonth.of(2026, 2));
    }

    @Test
    void calculate_crossesYearBoundary() {
        YearMonth result = LastInstallmentDateCalculator.calculate(
                YearMonth.of(2026, 11), 4);

        assertThat(result).isEqualTo(YearMonth.of(2027, 2));
    }

    @Test
    void calculate_installmentNumberLessThanTwo_throws() {
        assertThatThrownBy(() -> LastInstallmentDateCalculator.calculate(LocalDate.now(), 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 2");
    }

    @Test
    void calculate_nullDate_throws() {
        assertThatThrownBy(() -> LastInstallmentDateCalculator.calculate((LocalDate) null, 3))
                .isInstanceOf(NullPointerException.class);
    }
}
