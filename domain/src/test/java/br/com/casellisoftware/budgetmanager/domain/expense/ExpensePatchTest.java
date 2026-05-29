package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpensePatchTest {

    @Test
    void constructor_rejectsNullOptionals() {
        assertThatThrownBy(() -> new ExpensePatch(null, Optional.empty(), Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");

        assertThatThrownBy(() -> new ExpensePatch(Optional.empty(), null, Optional.empty()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cost");

        assertThatThrownBy(() -> new ExpensePatch(Optional.empty(), Optional.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("purchaseDate");
    }

    @Test
    void empty_returnsPatchWithoutAppliedFields() {
        ExpensePatch patch = ExpensePatch.empty();

        assertThat(patch.isEmpty()).isTrue();
        assertThat(patch.appliedFieldNames()).isEmpty();
    }

    @Test
    void withMethods_ignoreNullsAndApplyNamedFields() {
        ExpensePatch patch = ExpensePatch.empty()
                .withName("dinner")
                .withName(null)
                .withCost(Money.of("30.00"))
                .withCost(null)
                .withPurchaseDate(LocalDate.of(2026, 4, 22))
                .withPurchaseDate(null);

        assertThat(patch.isEmpty()).isFalse();
        assertThat(patch.name()).contains("dinner");
        assertThat(patch.cost()).contains(Money.of("30.00"));
        assertThat(patch.purchaseDate()).contains(LocalDate.of(2026, 4, 22));
        assertThat(patch.appliedFieldNames()).containsExactly("name", "cost", "purchaseDate");
    }
}
