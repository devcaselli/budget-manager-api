package br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.configs.MongoConfiguration;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.mappers.SubscriptionChargePersistenceMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({SubscriptionChargeRepositoryImpl.class, MongoConfiguration.class})
@ComponentScan(basePackageClasses = SubscriptionChargePersistenceMapper.class)
class SubscriptionChargeRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private SubscriptionChargeRepositoryImpl repository;

    @Autowired
    private MongoOperations mongoOperations;

    @Test
    void save_persistsFlattenedMoneyAndYearMonthString() {
        SubscriptionCharge charge = SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"), FlagEnum.NONE);

        SubscriptionCharge saved = repository.save(charge);

        Document document = mongoOperations
                .getCollection("subscription_charge")
                .find(new Document("_id", saved.getId()))
                .first();

        assertThat(document).isNotNull();
        assertThat(document.get("subscriptionId")).isEqualTo("subscription-1");
        assertThat(document.get("walletId")).isEqualTo("wallet-1");
        assertThat(document.get("month")).isEqualTo("2026-05");
        assertThat(document.get("amount").toString()).isEqualTo("99.90");
        assertThat(document.get("remaining").toString()).isEqualTo("99.90");
        assertThat(document.get("currency")).isEqualTo("BRL");
    }

    @Test
    void findById_whenFound_returnsMappedCharge() {
        SubscriptionCharge saved = repository.save(SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"), FlagEnum.NONE));

        Optional<SubscriptionCharge> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getSubscriptionId()).isEqualTo("subscription-1");
        assertThat(result.get().getWalletId()).isEqualTo("wallet-1");
        assertThat(result.get().getAmount()).isEqualTo(Money.of("99.90"));
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void findByWalletId_returnsOnlyChargesForWallet() {
        SubscriptionCharge walletOneFirst = repository.save(SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"), FlagEnum.NONE));
        SubscriptionCharge walletOneSecond = repository.save(SubscriptionCharge
                .create("subscription-2", "wallet-1", YearMonth.of(2026, 5), Money.of("20.00"), FlagEnum.NONE));
        SubscriptionCharge walletTwo = repository.save(SubscriptionCharge
                .create("subscription-3", "wallet-2", YearMonth.of(2026, 5), Money.of("30.00"), FlagEnum.NONE));

        List<SubscriptionCharge> result = repository.findByWalletId("wallet-1");

        assertThat(result)
                .extracting(SubscriptionCharge::getId)
                .containsExactlyInAnyOrder(walletOneFirst.getId(), walletOneSecond.getId())
                .doesNotContain(walletTwo.getId());
    }

    @Test
    void save_whenSameSubscriptionWalletAndMonthAlreadyExists_throwsDuplicateKeyException() {
        repository.save(SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"), FlagEnum.NONE));

        SubscriptionCharge duplicate = SubscriptionCharge
                .create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.of("99.90"), FlagEnum.NONE);

        assertThatThrownBy(() -> repository.save(duplicate))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
