package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionPatchTest {

    @Test
    void constructor_rejectsNullOptionals() {
        assertThatThrownBy(() -> new SubscriptionPatch(null, Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
        assertThatThrownBy(() -> new SubscriptionPatch(Optional.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("newAmount");
        assertThatThrownBy(() -> new SubscriptionPatch(Optional.empty(), Optional.empty(), null, Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("creditCardId");
    }

    @Test
    void withNullValues_areNoOps() {
        SubscriptionPatch patch = SubscriptionPatch.empty();

        assertThat(patch.withDescription(null)).isSameAs(patch);
        assertThat(patch.withNewAmount(null)).isSameAs(patch);
        assertThat(patch.withCreditCardId(null)).isSameAs(patch);
    }

    @Test
    void isEmpty_reflectsWhetherAnyFieldWasApplied() {
        assertThat(SubscriptionPatch.empty().isEmpty()).isTrue();
        assertThat(SubscriptionPatch.empty().withDescription("streaming").isEmpty()).isFalse();
        assertThat(SubscriptionPatch.empty().withNewAmount(Money.of("30.00")).isEmpty()).isFalse();
        assertThat(SubscriptionPatch.empty().withCreditCardId("cc-2").isEmpty()).isFalse();
    }
}
