package br.com.casellisoftware.budgetmanager.persistence.wallet;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.persistence.wallet.mappers.WalletPersistenceMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.LocalDate;
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
        return Wallet.create(
                description,
                Money.of(budget),
                null,
                LocalDate.of(2026, 1, 1),
                false
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
        Wallet main = repository.save(newWallet("Main wallet", "3000.00"));
        Wallet side = repository.save(newWallet("Side wallet", "100.00"));

        List<Wallet> result = repository.findAll();

        assertThat(result)
                .extracting(Wallet::getId)
                .contains(main.getId(), side.getId());
        assertThat(result)
                .extracting(Wallet::getDescription)
                .contains("Main wallet", "Side wallet");
    }
}
