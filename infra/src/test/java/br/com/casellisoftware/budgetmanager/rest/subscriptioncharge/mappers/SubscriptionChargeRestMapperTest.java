package br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.mappers;

import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos.SubscriptionChargeResponseDto;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionChargeRestMapperTest {

    private final SubscriptionChargeRestMapper mapper = Mappers.getMapper(SubscriptionChargeRestMapper.class);

    @Test
    void toResponse_copiesAllFields() {
        SubscriptionChargeOutput output = new SubscriptionChargeOutput(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                new BigDecimal("55.90"),
                new BigDecimal("20.00")
        );

        SubscriptionChargeResponseDto response = mapper.toResponse(output);

        assertThat(response.id()).isEqualTo("charge-1");
        assertThat(response.subscriptionId()).isEqualTo("subscription-1");
        assertThat(response.walletId()).isEqualTo("wallet-1");
        assertThat(response.month()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(response.amount()).isEqualByComparingTo("55.90");
        assertThat(response.remaining()).isEqualByComparingTo("20.00");
    }
}
