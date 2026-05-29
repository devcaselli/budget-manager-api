package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionOutputAssemblerTest {

    @Test
    void from_mapsDomainSubscriptionToBoundaryOutput() {
        Subscription subscription = Subscription.rebuild(
                "subscription-1",
                "streaming",
                Currency.getInstance("BRL"),
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 8),
                SubscriptionState.PRODUCTION,
                List.of(
                        new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00")),
                        new SubscriptionVersion(YearMonth.of(2026, 7), Money.of("65.00"))
                ),
                FlagEnum.NONE
        );

        SubscriptionOutput output = SubscriptionOutputAssembler.from(subscription);

        assertThat(output.id()).isEqualTo("subscription-1");
        assertThat(output.description()).isEqualTo("streaming");
        assertThat(output.currency()).isEqualTo("BRL");
        assertThat(output.startMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(output.endMonth()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(output.versions())
                .containsExactly(
                        new SubscriptionVersionOutput(YearMonth.of(2026, 5), Money.of("50.00").amount()),
                        new SubscriptionVersionOutput(YearMonth.of(2026, 7), Money.of("65.00").amount())
                );
        assertThatThrownBy(() -> output.versions().add(new SubscriptionVersionOutput(
                YearMonth.of(2026, 9),
                Money.of("70.00").amount())))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
