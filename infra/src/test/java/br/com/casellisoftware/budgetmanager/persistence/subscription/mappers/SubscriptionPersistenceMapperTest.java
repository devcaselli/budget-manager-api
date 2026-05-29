package br.com.casellisoftware.budgetmanager.persistence.subscription.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.persistence.subscription.SubscriptionDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionPersistenceMapperTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    private final SubscriptionPersistenceMapper mapper = Mappers.getMapper(SubscriptionPersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndLeavesVersionNull() {
        Subscription subscription = Subscription.create("Netflix", BRL, Money.of("55.90"), YearMonth.of(2026, 5),
                        SubscriptionState.PRODUCTION, FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, "cc-test")
                .addVersion(YearMonth.of(2026, 7), Money.of("60.00"))
                .endAt(YearMonth.of(2026, 9));

        SubscriptionDocument document = mapper.toDocument(subscription);

        assertThat(document.getId()).isEqualTo(subscription.getId());
        assertThat(document.getVersion()).isNull();
        assertThat(document.getDescription()).isEqualTo("Netflix");
        assertThat(document.getCurrency()).isEqualTo("BRL");
        assertThat(document.getStartMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(document.getEndMonth()).isEqualTo(YearMonth.of(2026, 9));
        assertThat(document.getVersions()).hasSize(2);
        assertThat(document.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        assertThat(document.getVersions().get(0).getEffectiveMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(document.getVersions().get(0).getAmount()).isEqualByComparingTo("55.90");
        assertThat(document.getVersions().get(1).getEffectiveMonth()).isEqualTo(YearMonth.of(2026, 7));
        assertThat(document.getVersions().get(1).getAmount()).isEqualByComparingTo("60.00");
    }

    @Test
    void toDocument_withVersionCopiesVersion() {
        Subscription subscription = Subscription.create("Cloud", BRL, Money.of("20.00"), YearMonth.of(2026, 5),
                        SubscriptionState.PRODUCTION, FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, "cc-test");

        SubscriptionDocument document = mapper.toDocument(subscription, 7L);

        assertThat(document.getVersion()).isEqualTo(7L);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Subscription original = Subscription.create("Music", BRL, Money.of("19.90"), YearMonth.of(2026, 5),
                        SubscriptionState.PRODUCTION, FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, "cc-test")
                .addVersion(YearMonth.of(2026, 8), Money.of("24.90"));

        Subscription roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void toDocument_whenSubscriptionIsNull_returnsNull() {
        assertThat(mapper.toDocument((Subscription) null)).isNull();
        assertThat(mapper.toDocument(null, 1L)).isNull();
    }

    @Test
    void toDomain_whenDocumentIsNull_returnsNull() {
        assertThat(mapper.toDomain((SubscriptionDocument) null)).isNull();
    }

    @Test
    void toDomain_whenVersionsAreNull_throwsDiagnosticException() {
        SubscriptionDocument document = new SubscriptionDocument(
                "subscription-1",
                null,
                "Netflix",
                "BRL",
                "PRODUCTION",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION,
                YearMonth.of(2026, 5),
                null,
                null
        );

        assertThatThrownBy(() -> mapper.toDomain(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subscription document versions must not be null or empty: id=subscription-1");
    }

    @Test
    void toDomain_whenVersionsAreEmpty_throwsDiagnosticException() {
        SubscriptionDocument document = new SubscriptionDocument(
                "subscription-1",
                null,
                "Netflix",
                "BRL",
                "PRODUCTION",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION,
                YearMonth.of(2026, 5),
                null,
                List.of()
        );

        assertThatThrownBy(() -> mapper.toDomain(document))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("subscription document versions must not be null or empty: id=subscription-1");
    }

    @Test
    void versionMapping_whenVersionIsNull_returnsNullBothWays() {
        assertThat(mapper.toDocument((SubscriptionVersion) null)).isNull();
        assertThat(mapper.toDomain(null, BRL)).isNull();
    }
}
