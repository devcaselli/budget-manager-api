package br.com.casellisoftware.budgetmanager.domain.payment;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentPatchTest {

    @Test
    void constructor_rejectsNullOptionals() {
        assertThatThrownBy(() -> new PaymentPatch(null, Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> new PaymentPatch(Optional.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("details");
    }

    @Test
    void withNullValues_areNoOps() {
        PaymentPatch patch = PaymentPatch.empty();

        assertThat(patch.withAmount(null)).isSameAs(patch);
        assertThat(patch.withDetails(null)).isSameAs(patch);
    }

    @Test
    void appliedFieldNames_returnsImmutableListInInsertionOrder() {
        PaymentPatch patch = PaymentPatch.empty()
                .withAmount(Money.of("10.00"))
                .withDetails("paid");

        assertThat(patch.appliedFieldNames()).containsExactly("amount", "details");
        assertThatThrownBy(() -> patch.appliedFieldNames().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
