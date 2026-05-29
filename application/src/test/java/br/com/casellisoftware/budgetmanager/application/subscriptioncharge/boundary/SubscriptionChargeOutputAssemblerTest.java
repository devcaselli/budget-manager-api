package br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionChargeOutputAssemblerTest {

    @Test
    void from_copiesAllFieldsAndFlattensMoneyToAmounts() {
        SubscriptionCharge charge = SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("80.00"), FlagEnum.NONE)
                .debit(Money.of("30.00"));

        SubscriptionChargeOutput output = SubscriptionChargeOutputAssembler.from(charge);

        assertThat(output.id()).isEqualTo(charge.getId());
        assertThat(output.subscriptionId()).isEqualTo("subscription-1");
        assertThat(output.walletId()).isEqualTo("wallet-1");
        assertThat(output.month()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(output.amount()).isEqualByComparingTo("80.00");
        assertThat(output.remaining()).isEqualByComparingTo("50.00");
    }
}
