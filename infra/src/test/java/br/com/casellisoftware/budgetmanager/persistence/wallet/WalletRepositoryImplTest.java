package br.com.casellisoftware.budgetmanager.persistence.wallet;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.persistence.wallet.mappers.WalletPersistenceMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(WalletRepositoryImpl.class)
@ComponentScan(basePackageClasses = WalletPersistenceMapper.class)
class WalletRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private WalletRepositoryImpl repository;

    @Autowired
    private MongoOperations mongoOperations;

    private static Wallet newWallet(String description, String budget) {
        return newWallet(description, budget, LocalDate.of(2026, 1, 1));
    }

    private static Wallet newWallet(String description, String budget, LocalDate startDate) {
        return Wallet.create(
                description,
                Money.of(budget),
                null,
                startDate,
                false,
                YearMonth.from(startDate),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }

    @Test
    void save_persistsFlattenedMoneyFields() {
        Wallet saved = repository.save(newWallet("Main wallet", "3000.00"));

        Document document = mongoOperations
                .getCollection("walletDocument")
                .find(new Document("_id", saved.getId()))
                .first();

        assertThat(document).isNotNull();
        assertThat(document)
                .containsKeys("budgetAmount", "budgetCurrency", "remainingAmount", "remainingCurrency")
                .doesNotContainKeys("budget", "remaining");
        assertThat(document.get("budgetAmount").toString()).isEqualTo("3000.00");
        assertThat(document.get("budgetCurrency")).isEqualTo("BRL");
        assertThat(document.get("remainingAmount").toString()).isEqualTo("3000.00");
        assertThat(document.get("remainingCurrency")).isEqualTo("BRL");
    }

    @Test
    void findById_whenFound_returnsMappedWallet() {
        Wallet saved = repository.save(newWallet("Side wallet", "100.00"));

        Optional<Wallet> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Side wallet");
        assertThat(result.get().getBudget().amount()).isEqualByComparingTo("100.00");
        assertThat(result.get().getRemaining().amount()).isEqualByComparingTo("100.00");
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("nonexistent-id")).isEmpty();
    }

    @Test
    void findAll_returnsMappedWallets() {
        Wallet main = repository.save(newWallet("Main wallet", "3000.00", LocalDate.of(2026, 1, 1)));
        Wallet side = repository.save(newWallet("Side wallet", "100.00", LocalDate.of(2026, 2, 1)));

        List<Wallet> result = repository.findAll();

        assertThat(result)
                .extracting(Wallet::getId)
                .contains(main.getId(), side.getId());
        assertThat(result)
                .extracting(Wallet::getDescription)
                .contains("Main wallet", "Side wallet");
    }

    @Test
    void findIdsByEffectiveMonth_returnsOnlyMatchingWalletIds() {
        Wallet april = repository.save(newWallet("April wallet", "100.00", LocalDate.of(2026, 4, 1)));
        Wallet may = repository.save(newWallet("May wallet", "100.00", LocalDate.of(2026, 5, 1)));
        Wallet july = repository.save(newWallet("July wallet", "100.00", LocalDate.of(2026, 7, 1)));

        List<String> ids = repository.findIdsByEffectiveMonth(YearMonth.of(2026, 5));

        assertThat(ids).containsExactly(may.getId());
        assertThat(ids).doesNotContain(april.getId(), july.getId());
    }

    @Test
    void existsOpenProductionFor_whenIsClosedNull_returnsTrue() {
        // Regression: docs persisted before 2026-05-15 backfill carried `isClosed: null`.
        // Old query `Criteria.where("isClosed").is(false)` missed them, allowing a 2nd
        // open PRODUCTION wallet for the same (ownerId, effectiveMonth). The current
        // query uses `ne(true)`, which must catch null/missing alongside false.
        Wallet wallet = repository.save(newWallet("Legacy wallet", "100.00", LocalDate.of(2026, 6, 1)));
        mongoOperations.getCollection("walletDocument").updateOne(
                new Document("_id", wallet.getId()),
                new Document("$unset", new Document("isClosed", ""))
        );

        boolean exists = repository.existsOpenProductionFor(
                YearMonth.of(2026, 6), LocalDate.of(2026, 6, 1), null);

        assertThat(exists).isTrue();
    }

    @Test
    void existsOpenProductionFor_whenIsClosedTrue_returnsFalse() {
        Wallet wallet = repository.save(newWallet("Closed wallet", "100.00", LocalDate.of(2026, 8, 1)));
        mongoOperations.getCollection("walletDocument").updateOne(
                new Document("_id", wallet.getId()),
                new Document("$set", new Document("isClosed", true))
        );

        boolean exists = repository.existsOpenProductionFor(
                YearMonth.of(2026, 8), LocalDate.of(2026, 8, 1), null);

        assertThat(exists).isFalse();
    }
}
