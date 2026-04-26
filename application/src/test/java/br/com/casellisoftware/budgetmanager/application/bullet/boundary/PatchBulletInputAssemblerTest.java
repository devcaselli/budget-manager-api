package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PatchBulletInputAssemblerTest {

    @Test
    void toPatch_mapsNonNullFieldsToDomainPatch() {
        var input = new PatchBulletInput(
                "bullet-1",
                "groceries",
                new BigDecimal("30.00"),
                new BigDecimal("12.50"),
                "wallet-1"
        );

        var patch = PatchBulletInputAssembler.toPatch(input);

        assertThat(patch.description()).contains("groceries");
        assertThat(patch.budget()).contains(Money.of("30.00"));
        assertThat(patch.remaining()).contains(Money.of("12.50"));
        assertThat(patch.walletId()).contains("wallet-1");
        assertThat(patch.appliedFieldNames())
                .containsExactly("description", "budget", "remaining", "walletId");
    }

    @Test
    void toPatch_whenOnlyDescriptionIsProvided_keepsOtherFieldsEmpty() {
        var input = new PatchBulletInput(
                "bullet-1",
                "groceries",
                null,
                null,
                null
        );

        var patch = PatchBulletInputAssembler.toPatch(input);

        assertThat(patch.description()).contains("groceries");
        assertThat(patch.budget()).isEmpty();
        assertThat(patch.remaining()).isEmpty();
        assertThat(patch.walletId()).isEmpty();
        assertThat(patch.appliedFieldNames()).containsExactly("description");
    }
}
