package br.com.casellisoftware.budgetmanager.domain.bullet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BulletTest {

    @Test
    void patch_onlyUpdatesNonNullFields() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Bullet patched = bullet.patch(BulletPatch.empty()
                .withDescription("groceries")
                .withBudget(Money.of("600.00"))
                .withWalletId("wallet-1"));

        assertThat(patched.getId()).isEqualTo("bullet-1");
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
        assertThat(patched.getDescription()).isEqualTo("groceries");
        assertThat(patched.getBudget()).isEqualTo(Money.of("600.00"));
        assertThat(patched.getRemaining()).isEqualTo(Money.of("320.00"));
    }

    @Test
    void patch_onlyDescriptionUpdatesDescriptionAndPreservesFinancialFields() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Bullet patched = bullet.patch(BulletPatch.empty().withDescription("groceries"));

        assertThat(patched.getDescription()).isEqualTo("groceries");
        assertThat(patched.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(patched.getRemaining()).isEqualTo(Money.of("320.00"));
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void patch_onlyBudgetUpdatesBudgetAndPreservesOtherFields() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Bullet patched = bullet.patch(BulletPatch.empty().withBudget(Money.of("650.00")));

        assertThat(patched.getDescription()).isEqualTo("rent");
        assertThat(patched.getBudget()).isEqualTo(Money.of("650.00"));
        assertThat(patched.getRemaining()).isEqualTo(Money.of("320.00"));
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void patch_onlyRemainingUpdatesRemainingAndPreservesOtherFields() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Bullet patched = bullet.patch(BulletPatch.empty().withRemaining(Money.of("400.00")));

        assertThat(patched.getDescription()).isEqualTo("rent");
        assertThat(patched.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(patched.getRemaining()).isEqualTo(Money.of("400.00"));
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void patch_allNull_returnsSameState() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Bullet patched = bullet.patch(BulletPatch.empty());

        assertThat(patched).isSameAs(bullet);
    }

    @Test
    void patch_sameValues_returnsSameState() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        Bullet patched = bullet.patch(BulletPatch.empty()
                .withDescription("rent")
                .withBudget(Money.of("500.00"))
                .withRemaining(Money.of("320.00")));

        assertThat(patched).isSameAs(bullet);
    }

    @Test
    void patch_rejectsNullPatch() {
        Bullet bullet = Bullet.create("rent", Money.of("500.00"), Money.of("500.00"), "wallet-1");

        assertThatThrownBy(() -> bullet.patch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("patch");
    }

    @Test
    void patch_rejectsRemainingGreaterThanBudget() {
        Bullet bullet = Bullet.create("rent", Money.of("500.00"), Money.of("500.00"), "wallet-1");

        assertThatThrownBy(() -> bullet.patch(BulletPatch.empty()
                .withBudget(Money.of("300.00"))
                .withRemaining(Money.of("301.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remaining must not exceed budget");
    }

    @Test
    void patch_rejectsWalletIdChange() {
        Bullet bullet = Bullet.create("rent", Money.of("500.00"), Money.of("500.00"), "wallet-1");

        assertThatThrownBy(() -> bullet.patch(BulletPatch.empty().withWalletId("wallet-2")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId is immutable");
    }

    @Test
    void patch_rejectsBlankDescription() {
        Bullet bullet = Bullet.create("rent", Money.of("500.00"), Money.of("500.00"), "wallet-1");

        assertThatThrownBy(() -> bullet.patch(BulletPatch.empty().withDescription("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
    }

    @Test
    void patch_rejectsNullDescriptionFromCurrentStateWithSpecificMessage() {
        Bullet bullet = new Bullet(
                "bullet-1",
                null,
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );

        assertThatThrownBy(() -> bullet.patch(BulletPatch.empty().withBudget(Money.of("600.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be null");
    }

    @Test
    void patch_rejectsNullWalletIdFromCurrentStateWithoutThrowingNpe() {
        Bullet bullet = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                null
        );

        assertThatThrownBy(() -> bullet.patch(BulletPatch.empty().withDescription("groceries")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId must not be null");
    }
}
