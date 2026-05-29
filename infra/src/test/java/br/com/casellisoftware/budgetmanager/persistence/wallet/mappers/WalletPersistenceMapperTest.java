package br.com.casellisoftware.budgetmanager.persistence.wallet.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class WalletPersistenceMapperTest {

    private final WalletPersistenceMapper mapper = Mappers.getMapper(WalletPersistenceMapper.class);

    @Test
    void toDocument_copiesAllFields_andLeavesVersionNull() {
        Wallet wallet = new Wallet(
                "id-1",
                "Main wallet",
                Money.of("3000.00"),
                Money.of("2500.00"),
                LocalDate.of(2026, 1, 1),
                null,
                false,
                YearMonth.of(2026, 1),
                WalletState.PRODUCTION,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        WalletDocument document = mapper.toDocument(wallet);

        assertThat(document.getId()).isEqualTo("id-1");
        assertThat(document.getDescription()).isEqualTo("Main wallet");
        assertThat(document.getBudgetAmount()).isEqualByComparingTo("3000.00");
        assertThat(document.getBudgetCurrency()).isEqualTo("BRL");
        assertThat(document.getRemainingAmount()).isEqualByComparingTo("2500.00");
        assertThat(document.getRemainingCurrency()).isEqualTo("BRL");
        assertThat(document.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(document.getClosedDate()).isNull();
        assertThat(document.getIsClosed()).isFalse();
        assertThat(document.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        // @Mapping(target = "version", ignore = true) — Spring Data populates it on save/find.
        assertThat(document.getVersion()).isNull();
    }

    @Test
    void toDomain_copiesAllFields_andIgnoresVersionSource() {
        WalletDocument document = new WalletDocument(
                "id-2",
                42L, // version present on the document — MUST NOT leak into domain
                "Side wallet",
                new BigDecimal("100.00"),
                "BRL",
                new BigDecimal("80.00"),
                "BRL",
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 12, 31),
                true,
                java.time.YearMonth.of(2026, 2),
                "PRODUCTION"
        );
        document.setFlag(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);

        Wallet wallet = mapper.toDomain(document);

        assertThat(wallet.getId()).isEqualTo("id-2");
        assertThat(wallet.getDescription()).isEqualTo("Side wallet");
        assertThat(wallet.getBudget().amount()).isEqualByComparingTo("100.00");
        assertThat(wallet.getRemaining().amount()).isEqualByComparingTo("80.00");
        assertThat(wallet.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(wallet.getClosedDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(wallet.getClosed()).isTrue();
        assertThat(wallet.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Wallet original = new Wallet(
                "id-3",
                "Round trip",
                Money.of("500.00"),
                Money.of("500.00"),
                LocalDate.of(2026, 3, 1),
                null,
                false,
                YearMonth.of(2026, 3),
                WalletState.PRODUCTION,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        Wallet roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }
}
