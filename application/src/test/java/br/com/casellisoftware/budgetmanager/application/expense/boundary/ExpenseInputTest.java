package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseInputTest {

    private static final Instant NOW = Instant.parse("2026-04-07T12:00:00Z");

    @Test
    void validInput_isConstructed() {
        ExpenseInput input = new ExpenseInput("lunch", new BigDecimal("10.00"), NOW, "wallet-1");

        assertThat(input.name()).isEqualTo("lunch");
        assertThat(input.cost()).isEqualByComparingTo("10.00");
        assertThat(input.purchaseDate()).isEqualTo(NOW);
        assertThat(input.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void nullName_throws() {
        assertThatThrownBy(() -> new ExpenseInput(null, BigDecimal.ONE, NOW, "wallet-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void blankName_throws() {
        assertThatThrownBy(() -> new ExpenseInput("   ", BigDecimal.ONE, NOW, "wallet-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void nullCost_throws() {
        assertThatThrownBy(() -> new ExpenseInput("lunch", null, NOW, "wallet-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cost");
    }

    @Test
    void zeroCost_throws() {
        assertThatThrownBy(() -> new ExpenseInput("lunch", BigDecimal.ZERO, NOW, "wallet-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void negativeCost_throws() {
        assertThatThrownBy(() -> new ExpenseInput("lunch", new BigDecimal("-1"), NOW, "wallet-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void nullWalletId_throws() {
        assertThatThrownBy(() -> new ExpenseInput("lunch", BigDecimal.ONE, NOW, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void blankWalletId_throws() {
        assertThatThrownBy(() -> new ExpenseInput("lunch", BigDecimal.ONE, NOW, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void nullPurchaseDate_isAllowed() {
        ExpenseInput input = new ExpenseInput("lunch", BigDecimal.ONE, null, "wallet-1");
        assertThat(input.purchaseDate()).isNull();
    }
}
