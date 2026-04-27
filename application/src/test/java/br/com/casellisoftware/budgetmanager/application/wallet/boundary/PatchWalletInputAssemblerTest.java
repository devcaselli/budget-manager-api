package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatchWalletInputAssemblerTest {

    @Test
    void toPatch_mapsOnlyPatchableNonNullFieldsToDomainPatch() {
        var input = new PatchWalletInput(
                "wallet-1",
                "may",
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true
        );

        var patch = PatchWalletInputAssembler.toPatch(input);

        assertThat(patch.description()).contains("may");
        assertThat(patch.budget()).contains(Money.of("1200.00"));
        assertThat(patch.closedDate()).contains(LocalDate.of(2026, 5, 31));
        assertThat(patch.closed()).contains(true);
        assertThat(patch.appliedFieldNames())
                .containsExactly("description", "budget", "closedDate", "closed");
    }

    @Test
    void toPatch_ignoresStartDateBecauseItIsImmutable() {
        var input = new PatchWalletInput(
                "wallet-1",
                null,
                null,
                LocalDate.of(2026, 5, 1),
                null,
                null
        );

        var patch = PatchWalletInputAssembler.toPatch(input);

        assertThat(patch.isEmpty()).isTrue();
        assertThat(patch.appliedFieldNames()).isEmpty();
    }
}
