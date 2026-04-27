package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PatchPaymentInputAssemblerTest {

    @Test
    void toPatch_mapsNonNullFieldsToDomainPatch() {
        var input = new PatchPaymentInput(
                "payment-1",
                new BigDecimal("30.00"),
                "updated"
        );

        var patch = PatchPaymentInputAssembler.toPatch(input);

        assertThat(patch.amount()).contains(Money.of("30.00"));
        assertThat(patch.details()).contains("updated");
        assertThat(patch.appliedFieldNames()).containsExactly("amount", "details");
    }

    @Test
    void toPatch_whenOnlyDetailsIsProvided_keepsAmountEmpty() {
        var input = new PatchPaymentInput(
                "payment-1",
                null,
                "updated"
        );

        var patch = PatchPaymentInputAssembler.toPatch(input);

        assertThat(patch.amount()).isEmpty();
        assertThat(patch.details()).contains("updated");
        assertThat(patch.appliedFieldNames()).containsExactly("details");
    }
}
