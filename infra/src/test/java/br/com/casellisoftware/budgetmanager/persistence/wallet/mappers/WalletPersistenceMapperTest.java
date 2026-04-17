package br.com.casellisoftware.budgetmanager.persistence.wallet.mappers;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;

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
                false
        );

        WalletDocument document = mapper.toDocument(wallet);

        assertThat(document.getId()).isEqualTo("id-1");
        assertThat(document.getDescription()).isEqualTo("Main wallet");
        assertThat(document.getBudget().amount()).isEqualByComparingTo("3000.00");
        assertThat(document.getRemaining().amount()).isEqualByComparingTo("2500.00");
        assertThat(document.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(document.getClosedDate()).isNull();
        assertThat(document.getIsClosed()).isFalse();
        // @Mapping(target = "version", ignore = true) — Spring Data populates it on save/find.
        assertThat(document.getVersion()).isNull();
    }

    @Test
    void toDomain_copiesAllFields_andIgnoresVersionSource() {
        WalletDocument document = new WalletDocument(
                "id-2",
                42L, // version present on the document — MUST NOT leak into domain
                "Side wallet",
                Money.of("100.00"),
                Money.of("80.00"),
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 12, 31),
                true
        );

        Wallet wallet = mapper.toDomain(document);

        assertThat(wallet.getId()).isEqualTo("id-2");
        assertThat(wallet.getDescription()).isEqualTo("Side wallet");
        assertThat(wallet.getBudget().amount()).isEqualByComparingTo("100.00");
        assertThat(wallet.getRemaining().amount()).isEqualByComparingTo("80.00");
        assertThat(wallet.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(wallet.getClosedDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(wallet.getClosed()).isTrue();
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
                false
        );

        Wallet roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }
}
