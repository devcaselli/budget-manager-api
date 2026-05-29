package br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.SubscriptionChargeDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionChargePersistenceMapperTest {

    private final SubscriptionChargePersistenceMapper mapper = Mappers.getMapper(SubscriptionChargePersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndLeavesVersionNull() {
        SubscriptionCharge charge = SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"),
                        FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION)
                .debit(Money.of("20.00"));

        SubscriptionChargeDocument document = mapper.toDocument(charge);

        assertThat(document.getId()).isEqualTo(charge.getId());
        assertThat(document.getVersion()).isNull();
        assertThat(document.getSubscriptionId()).isEqualTo("subscription-1");
        assertThat(document.getWalletId()).isEqualTo("wallet-1");
        assertThat(document.getMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(document.getAmount()).isEqualByComparingTo("99.90");
        assertThat(document.getRemaining()).isEqualByComparingTo("79.90");
        assertThat(document.getCurrency()).isEqualTo("BRL");
        assertThat(document.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
    }

    @Test
    void toDocument_withVersionCopiesVersion() {
        SubscriptionCharge charge = SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"),
                        FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);

        SubscriptionChargeDocument document = mapper.toDocument(charge, 3L);

        assertThat(document.getVersion()).isEqualTo(3L);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        SubscriptionCharge original = SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"),
                        FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION)
                .debit(Money.of("20.00"));

        SubscriptionCharge roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void toDocument_whenSubscriptionChargeIsNull_returnsNull() {
        assertThat(mapper.toDocument(null)).isNull();
        assertThat(mapper.toDocument(null, 1L)).isNull();
    }

    @Test
    void toDomain_whenDocumentIsNull_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }
}
