package br.com.casellisoftware.budgetmanager.domain.bullet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BulletPatchTest {

    @Test
    void constructor_rejectsNullOptionals() {
        assertThatThrownBy(() -> new BulletPatch(null, Optional.empty(), Optional.empty(), Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
        assertThatThrownBy(() -> new BulletPatch(Optional.empty(), null, Optional.empty(), Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("budget");
        assertThatThrownBy(() -> new BulletPatch(Optional.empty(), Optional.empty(), null, Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("remaining");
        assertThatThrownBy(() -> new BulletPatch(Optional.empty(), Optional.empty(), Optional.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void withNullValues_areNoOps() {
        BulletPatch patch = BulletPatch.empty();

        assertThat(patch.withDescription(null)).isSameAs(patch);
        assertThat(patch.withBudget(null)).isSameAs(patch);
        assertThat(patch.withRemaining(null)).isSameAs(patch);
        assertThat(patch.withWalletId(null)).isSameAs(patch);
    }

    @Test
    void isEmpty_reflectsWhetherAnyFieldWasApplied() {
        assertThat(BulletPatch.empty().isEmpty()).isTrue();
        assertThat(BulletPatch.empty().withDescription("rent").isEmpty()).isFalse();
        assertThat(BulletPatch.empty().withBudget(Money.of("10.00")).isEmpty()).isFalse();
        assertThat(BulletPatch.empty().withRemaining(Money.of("5.00")).isEmpty()).isFalse();
        assertThat(BulletPatch.empty().withWalletId("wallet-1").isEmpty()).isFalse();
    }

    @Test
    void appliedFieldNames_returnsImmutableListInInsertionOrder() {
        BulletPatch patch = BulletPatch.empty()
                .withDescription("rent")
                .withBudget(Money.of("100.00"))
                .withRemaining(Money.of("60.00"))
                .withWalletId("wallet-1");

        assertThat(patch.appliedFieldNames())
                .containsExactly("description", "budget", "remaining", "walletId");
        assertThatThrownBy(() -> patch.appliedFieldNames().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
