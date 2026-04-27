package br.com.casellisoftware.budgetmanager.domain.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletPatchTest {

    @Test
    void constructor_rejectsNullOptionals() {
        assertThatThrownBy(() -> new WalletPatch(null, Optional.empty(), Optional.empty(), Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
        assertThatThrownBy(() -> new WalletPatch(Optional.empty(), null, Optional.empty(), Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("budget");
        assertThatThrownBy(() -> new WalletPatch(Optional.empty(), Optional.empty(), null, Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("closedDate");
        assertThatThrownBy(() -> new WalletPatch(Optional.empty(), Optional.empty(), Optional.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void withNullValues_areNoOps() {
        WalletPatch patch = WalletPatch.empty();

        assertThat(patch.withDescription(null)).isSameAs(patch);
        assertThat(patch.withBudget(null)).isSameAs(patch);
        assertThat(patch.withClosedDate(null)).isSameAs(patch);
        assertThat(patch.withClosed(null)).isSameAs(patch);
    }

    @Test
    void appliedFieldNames_returnsImmutableListInInsertionOrder() {
        WalletPatch patch = WalletPatch.empty()
                .withDescription("monthly")
                .withBudget(Money.of("100.00"))
                .withClosedDate(LocalDate.of(2026, 12, 31))
                .withClosed(true);

        assertThat(patch.appliedFieldNames())
                .containsExactly("description", "budget", "closedDate", "closed");
        assertThatThrownBy(() -> patch.appliedFieldNames().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
