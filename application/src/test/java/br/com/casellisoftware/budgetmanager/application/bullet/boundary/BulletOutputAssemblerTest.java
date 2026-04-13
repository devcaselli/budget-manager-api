package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulletOutputAssemblerTest {

    @Test
    void from_copiesAllFields_andFlattensMoneyToAmount() {
        Money budget = Money.of("500.00");
        Bullet bullet = Bullet.create("rent", budget, budget, "wallet-1");

        BulletOutput output = BulletOutputAssembler.from(bullet);

        assertThat(output.id()).isEqualTo(bullet.getId());
        assertThat(output.description()).isEqualTo("rent");
        assertThat(output.budget()).isEqualByComparingTo("500.00");
        assertThat(output.remaining()).isEqualByComparingTo("500.00");
        assertThat(output.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void from_afterDebit_reflectsReducedRemaining() {
        Money budget = Money.of("200.00");
        Bullet original = Bullet.create("groceries", budget, budget, "wallet-1");
        Bullet debited = original.debit(Money.of("50.00"));

        BulletOutput output = BulletOutputAssembler.from(debited);

        assertThat(output.budget()).isEqualByComparingTo("200.00");
        assertThat(output.remaining()).isEqualByComparingTo("150.00");
    }
}
