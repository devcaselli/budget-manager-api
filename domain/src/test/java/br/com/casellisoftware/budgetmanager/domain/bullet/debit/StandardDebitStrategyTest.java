package br.com.casellisoftware.budgetmanager.domain.bullet.debit;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StandardDebitStrategyTest {

    private final StandardDebitStrategy strategy = new StandardDebitStrategy();

    @Test
    void applyDebit_debitsCurrentRemaining() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Money remaining = strategy.applyDebit(bullet, Money.of("120.00"));

        assertThat(remaining).isEqualTo(Money.of("200.00"));
    }

    @Test
    void applyDebit_preservesStandardDebitValidation() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        assertThatThrownBy(() -> strategy.applyDebit(bullet, Money.of("321.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds remaining");
    }
}
