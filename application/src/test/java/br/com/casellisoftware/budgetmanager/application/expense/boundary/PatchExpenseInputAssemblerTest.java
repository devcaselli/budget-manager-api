package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PatchExpenseInputAssemblerTest {

    @Test
    void toPatch_mapsNonNullFieldsToDomainPatch() {
        var input = new PatchExpenseInput(
                "expense-1",
                "dinner",
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 22)
        );

        var patch = PatchExpenseInputAssembler.toPatch(input);

        assertThat(patch.name()).contains("dinner");
        assertThat(patch.cost()).contains(Money.of("30.00"));
        assertThat(patch.purchaseDate()).contains(LocalDate.of(2026, 4, 22));
        assertThat(patch.appliedFieldNames()).containsExactly("name", "cost", "purchaseDate");
    }

    @Test
    void toPatch_whenCostIsNull_keepsCostEmpty() {
        var input = new PatchExpenseInput(
                "expense-1",
                "dinner",
                null,
                null
        );

        var patch = PatchExpenseInputAssembler.toPatch(input);

        assertThat(patch.name()).contains("dinner");
        assertThat(patch.cost()).isEmpty();
        assertThat(patch.purchaseDate()).isEmpty();
        assertThat(patch.appliedFieldNames()).containsExactly("name");
    }
}
