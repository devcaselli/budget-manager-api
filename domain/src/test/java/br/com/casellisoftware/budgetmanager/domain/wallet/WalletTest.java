package br.com.casellisoftware.budgetmanager.domain.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    void patch_onlyUpdatesPatchableFields() {
        Wallet wallet = new Wallet(
                "wallet-1",
                "monthly",
                Money.of("1000.00"),
                Money.of("700.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                false
        );

        Wallet patched = wallet.patch(WalletPatch.empty()
                .withDescription("may")
                .withBudget(Money.of("1200.00"))
                .withClosedDate(LocalDate.of(2026, 5, 31))
                .withClosed(true));

        assertThat(patched.getId()).isEqualTo("wallet-1");
        assertThat(patched.getDescription()).isEqualTo("may");
        assertThat(patched.getBudget()).isEqualTo(Money.of("1200.00"));
        assertThat(patched.getRemaining()).isEqualTo(Money.of("700.00"));
        assertThat(patched.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(patched.getClosedDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(patched.getClosed()).isTrue();
    }

    @Test
    void patch_whenEmpty_returnsSameInstance() {
        Wallet wallet = Wallet.create(
                "monthly",
                Money.of("1000.00"),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 4, 1),
                false
        );

        assertThat(wallet.patch(WalletPatch.empty())).isSameAs(wallet);
    }

    @Test
    void patch_whenSameValues_returnsSameInstance() {
        Wallet wallet = new Wallet(
                "wallet-1",
                "monthly",
                Money.of("1000.00"),
                Money.of("700.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                false
        );

        Wallet patched = wallet.patch(WalletPatch.empty()
                .withDescription("monthly")
                .withBudget(Money.of("1000.00"))
                .withClosedDate(LocalDate.of(2026, 4, 30))
                .withClosed(false));

        assertThat(patched).isSameAs(wallet);
    }

    @Test
    void patch_rejectsNullPatch() {
        Wallet wallet = Wallet.create(
                "monthly",
                Money.of("1000.00"),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 4, 1),
                false
        );

        assertThatThrownBy(() -> wallet.patch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("patch");
    }

    @Test
    void patch_rejectsBudgetLowerThanRemaining() {
        Wallet wallet = new Wallet(
                "wallet-1",
                "monthly",
                Money.of("1000.00"),
                Money.of("700.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                false
        );

        assertThatThrownBy(() -> wallet.patch(WalletPatch.empty().withBudget(Money.of("600.00"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remaining must not exceed budget");
    }
}
