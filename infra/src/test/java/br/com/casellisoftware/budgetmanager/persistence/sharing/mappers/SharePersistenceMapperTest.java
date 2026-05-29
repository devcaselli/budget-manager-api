package br.com.casellisoftware.budgetmanager.persistence.sharing.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;
import br.com.casellisoftware.budgetmanager.persistence.sharing.ShareDocument;
import br.com.casellisoftware.budgetmanager.persistence.sharing.ShareQuotaDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the on-the-wire string discriminators for {@link ShareSourceType} and
 * {@link ShareStatus}. Renaming any enum constant breaks queries against
 * historical Mongo records — this test fails first, guarding the migration.
 */
class SharePersistenceMapperTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Instant CREATED_AT = Instant.parse("2026-05-16T10:00:00Z");

    private final SharePersistenceMapper mapper = new SharePersistenceMapper();

    @ParameterizedTest
    @CsvSource({
            "EXPENSE",
            "INSTALLMENT",
            "SUBSCRIPTION"
    })
    void sourceTypeNamePinnedOnWire(String expected) {
        assertThat(ShareSourceType.valueOf(expected).name()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
            "ACTIVE",
            "REVERTED"
    })
    void statusNamePinnedOnWire(String expected) {
        assertThat(ShareStatus.valueOf(expected).name()).isEqualTo(expected);
    }

    @Test
    void toDocument_persistsEnumNamesAsStrings() {
        Share share = activeShare(ShareSourceType.INSTALLMENT);

        ShareDocument document = mapper.toDocument(share, null);

        assertThat(document.getSourceType()).isEqualTo("INSTALLMENT");
        assertThat(document.getStatus()).isEqualTo("ACTIVE");
        assertThat(document.getTotalCurrency()).isEqualTo("BRL");
        assertThat(document.getOwnerCurrency()).isEqualTo("BRL");
    }

    @Test
    void toDomain_revivesEnumNamesFromStrings() {
        ShareDocument document = new ShareDocument(
                "share-1",
                "owner-1",
                "wallet-1",
                "SUBSCRIPTION",
                "source-1",
                new BigDecimal("100.00"),
                "BRL",
                new BigDecimal("60.00"),
                "BRL",
                ratio("0.6"),
                List.of(new ShareQuotaDocument("payer-1", ratio("0.4"), List.of())),
                "REVERTED",
                List.of(),
                CREATED_AT,
                CREATED_AT.plusSeconds(60),
                null
        );

        Share domain = mapper.toDomain(document);

        assertThat(domain.getSourceType()).isEqualTo(ShareSourceType.SUBSCRIPTION);
        assertThat(domain.getStatus()).isEqualTo(ShareStatus.REVERTED);
    }

    @Test
    void roundTripPreservesEnumNames() {
        Share share = activeShare(ShareSourceType.EXPENSE);

        ShareDocument document = mapper.toDocument(share, null);
        Share roundTripped = mapper.toDomain(document);

        assertThat(roundTripped.getSourceType()).isEqualTo(share.getSourceType());
        assertThat(roundTripped.getStatus()).isEqualTo(share.getStatus());
    }

    private Share activeShare(ShareSourceType sourceType) {
        Money total = Money.of(new BigDecimal("100.00"), BRL);
        Money ownerShare = Money.of(new BigDecimal("60.00"), BRL);
        List<Share.ShareQuotaAllocation> allocations = List.of(
                new Share.ShareQuotaAllocation("payer-1", Money.of(new BigDecimal("40.00"), BRL))
        );
        return Share.create(
                "wallet-1",
                sourceType,
                "source-1",
                total,
                ownerShare,
                allocations,
                "owner-1",
                CREATED_AT
        );
    }

    private static BigDecimal ratio(String value) {
        return new BigDecimal(value).setScale(Share.RATIO_SCALE, RoundingMode.HALF_EVEN);
    }
}
