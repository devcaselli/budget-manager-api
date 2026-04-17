package br.com.casellisoftware.budgetmanager.persistence.bullet.mappers;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.bullet.BulletDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BulletPersistenceMapperTest {

    private final BulletPersistenceMapper mapper = Mappers.getMapper(BulletPersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndCurrency() {
        Money budget = Money.of("500.00");
        Bullet bullet = Bullet.create("rent", budget, budget, "wallet-1");

        BulletDocument document = mapper.toDocument(bullet);

        assertThat(document.getId()).isEqualTo(bullet.getId());
        assertThat(document.getDescription()).isEqualTo("rent");
        assertThat(document.getBudget()).isEqualByComparingTo("500.00");
        assertThat(document.getRemaining()).isEqualByComparingTo("500.00");
        assertThat(document.getCurrency()).isEqualTo("BRL");
        assertThat(document.getWalletId()).isEqualTo("wallet-1");
        assertThat(document.getVersion()).isNull();
    }

    @Test
    void toDomain_copiesAllFields() {
        BulletDocument document = new BulletDocument(
                "id-1", null, "groceries", new BigDecimal("300.00"),
                new BigDecimal("150.00"), "BRL", "wallet-2"
        );

        Bullet bullet = mapper.toDomain(document);

        assertThat(bullet.getId()).isEqualTo("id-1");
        assertThat(bullet.getDescription()).isEqualTo("groceries");
        assertThat(bullet.getBudget().amount()).isEqualByComparingTo("300.00");
        assertThat(bullet.getBudget().currency().getCurrencyCode()).isEqualTo("BRL");
        assertThat(bullet.getRemaining().amount()).isEqualByComparingTo("150.00");
        assertThat(bullet.getWalletId()).isEqualTo("wallet-2");
    }

    @Test
    void toDomain_whenCurrencyMissing_fallsBackToDefault() {
        BulletDocument document = new BulletDocument(
                "id-2", null, "legacy", new BigDecimal("1.00"),
                new BigDecimal("1.00"), null, "wallet-legacy"
        );

        Bullet bullet = mapper.toDomain(document);

        assertThat(bullet.getBudget().currency()).isEqualTo(Money.DEFAULT_CURRENCY);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Money budget = Money.of("42.00");
        Bullet original = Bullet.create("dinner", budget, budget, "wallet-1");

        Bullet roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }
}
