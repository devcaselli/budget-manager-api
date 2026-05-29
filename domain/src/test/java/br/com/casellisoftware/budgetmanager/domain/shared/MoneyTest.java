package br.com.casellisoftware.budgetmanager.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void of_normalizesScaleToTwo() {
        assertThat(Money.of("10").amount()).isEqualByComparingTo("10.00");
        assertThat(Money.of("10.1").amount()).isEqualByComparingTo("10.10");
    }

    @Test
    void of_roundsHalfEven() {
        // 2.125 -> 2.12 (banker's rounding, even neighbor)
        assertThat(Money.of("2.125").amount()).isEqualByComparingTo("2.12");
        // 2.135 -> 2.14
        assertThat(Money.of("2.135").amount()).isEqualByComparingTo("2.14");
    }

    @Test
    void of_rejectsNullAmount() {
        assertThatThrownBy(() -> Money.of((BigDecimal) null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void of_rejectsNegative() {
        assertThatThrownBy(() -> Money.of("-0.01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void zero_isZeroAndNotPositive() {
        Money zero = Money.zero();
        assertThat(zero.isZero()).isTrue();
        assertThat(zero.isPositive()).isFalse();
    }

    @Test
    void zero_withCurrency_returnsZeroWithRequestedCurrency() {
        Money zero = Money.zero(BRL);
        assertThat(zero.isZero()).isTrue();
        assertThat(zero.currency()).isEqualTo(BRL);
    }

    @Test
    void add_returnsSum() {
        assertThat(Money.of("10.00").add(Money.of("2.50")).amount())
                .isEqualByComparingTo("12.50");
    }

    @Test
    void subtract_returnsDifference() {
        assertThat(Money.of("10.00").subtract(Money.of("2.50")).amount())
                .isEqualByComparingTo("7.50");
    }

    @Test
    void subtract_rejectsIfResultIsNegative() {
        assertThatThrownBy(() -> Money.of("2").subtract(Money.of("5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void arithmetic_rejectsCurrencyMismatch() {
        Money brl = Money.of(new BigDecimal("10"), BRL);
        Money usd = Money.of(new BigDecimal("10"), USD);
        assertThatThrownBy(() -> brl.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void isGreaterThan_comparesAmounts() {
        assertThat(Money.of("10.01").isGreaterThan(Money.of("10.00"))).isTrue();
        assertThat(Money.of("10.00").isGreaterThan(Money.of("10.00"))).isFalse();
    }

    @Test
    void equals_isBasedOnNormalizedAmountAndCurrency() {
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of("10").hashCode()).isEqualTo(Money.of("10.00").hashCode());
    }

    @Test
    void equals_differsByCurrency() {
        assertThat(Money.of(new BigDecimal("10"), BRL))
                .isNotEqualTo(Money.of(new BigDecimal("10"), USD));
    }

    @Test
    void debitBy_subtractsAmount() {
        assertThat(Money.of("10.00").debitBy(Money.of("3.00")).amount())
                .isEqualByComparingTo("7.00");
    }

    @Test
    void debitBy_rejectsNonPositive() {
        assertThatThrownBy(() -> Money.of("10").debitBy(Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debit amount must be positive");
    }

    @Test
    void debitBy_rejectsAmountExceedingRemaining() {
        assertThatThrownBy(() -> Money.of("5").debitBy(Money.of("6")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds remaining");
    }

    @Test
    void creditBy_addsAmount() {
        assertThat(Money.of("10.00").creditBy(Money.of("3.00")).amount())
                .isEqualByComparingTo("13.00");
    }

    @Test
    void creditBy_rejectsNonPositive() {
        assertThatThrownBy(() -> Money.of("10").creditBy(Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit amount must be positive");
    }

    @Test
    void creditBy_rejectsCurrencyMismatch() {
        Money brl = Money.of(new BigDecimal("10"), BRL);
        Money usd = Money.of(new BigDecimal("1"), USD);
        assertThatThrownBy(() -> brl.creditBy(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency mismatch");
    }

    @Test
    void creditBy_isSymmetricInverseOfDebitBy() {
        Money initial = Money.of("100.00");
        Money roundTripped = initial.debitBy(Money.of("40.00")).creditBy(Money.of("40.00"));
        assertThat(roundTripped).isEqualTo(initial);
    }
}
