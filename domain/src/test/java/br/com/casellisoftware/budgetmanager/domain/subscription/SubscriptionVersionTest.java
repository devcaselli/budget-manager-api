package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionVersionTest {

    @Test
    void constructor_normalizesAmountScale() {
        SubscriptionVersion version = new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("10.555"));

        assertThat(version.amount()).isEqualTo(Money.of("10.56"));
    }

    @Test
    void constructor_rejectsInvalidRequiredFields() {
        assertThatThrownBy(() -> new SubscriptionVersion(null, Money.of("10.00")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("effectiveMonth");
        assertThatThrownBy(() -> new SubscriptionVersion(YearMonth.of(2026, 5), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> new SubscriptionVersion(YearMonth.of(2026, 5), Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }
}
