package br.com.casellisoftware.budgetmanager.persistence.subscription;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.configs.MongoConfiguration;
import br.com.casellisoftware.budgetmanager.configs.SharedBeanConfiguration;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.persistence.subscription.mappers.SubscriptionPersistenceMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;

import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({SubscriptionRepositoryImpl.class, MongoConfiguration.class, SharedBeanConfiguration.class})
@ComponentScan(basePackageClasses = SubscriptionPersistenceMapper.class)
class SubscriptionRepositoryImplTest extends AbstractMongoIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Autowired
    private SubscriptionRepositoryImpl repository;

    @Autowired
    private MongoOperations mongoOperations;

    @Test
    void save_persistsSubscriptionWithEmbeddedVersionsAndYearMonthStrings() {
        Subscription subscription = Subscription.create("Netflix", BRL, Money.of("55.90"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test")
                .addVersion(YearMonth.of(2026, 7), Money.of("60.00"));

        Subscription saved = repository.save(subscription);

        Document document = mongoOperations
                .getCollection("subscription")
                .find(new Document("_id", saved.getId()))
                .first();

        assertThat(document).isNotNull();
        assertThat(document.get("description")).isEqualTo("Netflix");
        assertThat(document.get("currency")).isEqualTo("BRL");
        assertThat(document.get("startMonth")).isEqualTo("2026-05");
        assertThat(document.get("endMonth")).isNull();

        List<?> versions = document.getList("versions", Object.class);
        assertThat(versions).hasSize(2);
        Document firstVersion = (Document) versions.get(0);
        assertThat(firstVersion.get("effectiveMonth")).isEqualTo("2026-05");
        assertThat(firstVersion.get("amount").toString()).isEqualTo("55.90");
    }

    @Test
    void findById_whenFound_returnsMappedSubscription() {
        Subscription saved = repository.save(Subscription.create("Cloud", BRL, Money.of("20.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));

        Optional<Subscription> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Cloud");
        assertThat(result.get().resolveAmount(YearMonth.of(2026, 6))).isEqualTo(Money.of("20.00"));
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void save_secondSave_incrementsVersionInDocument() {
        Subscription saved = repository.save(Subscription.create("Cloud", BRL, Money.of("20.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));

        repository.save(saved.rename("Cloud Pro"));

        Document document = mongoOperations
                .getCollection("subscription")
                .find(new Document("_id", saved.getId()))
                .first();

        assertThat(document).isNotNull();
        assertThat(((Number) document.get("version")).longValue()).isEqualTo(1L);
        assertThat(document.get("description")).isEqualTo("Cloud Pro");
    }

    @Test
    void findActiveFor_returnsOnlySubscriptionsApplicableToMonth() {
        Subscription mayActive = repository.save(Subscription.create("May active", BRL, Money.of("10.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));
        Subscription endedBeforeJuly = repository.save(Subscription.create("Ended", BRL, Money.of("20.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test")
                .endAt(YearMonth.of(2026, 7)));
        Subscription future = repository.save(Subscription.create("Future", BRL, Money.of("30.00"), YearMonth.of(2026, 8), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));
        Subscription endingAfterJuly = repository.save(Subscription.create("Ending later", BRL, Money.of("40.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test")
                .endAt(YearMonth.of(2026, 8)));

        List<Subscription> result = repository.findActiveFor(YearMonth.of(2026, 7));

        assertThat(result)
                .extracting(Subscription::getId)
                .containsExactlyInAnyOrder(mayActive.getId(), endingAfterJuly.getId())
                .doesNotContain(endedBeforeJuly.getId(), future.getId());
    }

    @Test
    void findActiveFor_withState_keepsPreviewOutOfProductionAndIncludesItForPreviewWallets() {
        Subscription production = repository.save(Subscription.create(
                "Production",
                BRL,
                Money.of("10.00"),
                YearMonth.of(2026, 5),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE, "cc-test"));
        Subscription preview = repository.save(Subscription.create(
                "Preview",
                BRL,
                Money.of("20.00"),
                YearMonth.of(2026, 5),
                SubscriptionState.PREVIEW,
                FlagEnum.NONE, "cc-test"));
        Subscription future = repository.save(Subscription.create(
                "Future preview",
                BRL,
                Money.of("30.00"),
                YearMonth.of(2026, 8),
                SubscriptionState.PREVIEW,
                FlagEnum.NONE, "cc-test"));

        List<Subscription> productionResult = repository.findActiveFor(YearMonth.of(2026, 7), SubscriptionState.PRODUCTION);
        List<Subscription> previewResult = repository.findActiveFor(YearMonth.of(2026, 7), SubscriptionState.PREVIEW);

        assertThat(productionResult)
                .extracting(Subscription::getId)
                .contains(production.getId())
                .doesNotContain(preview.getId(), future.getId());
        assertThat(previewResult)
                .extracting(Subscription::getId)
                .containsExactlyInAnyOrder(production.getId(), preview.getId())
                .doesNotContain(future.getId());
    }

    @Test
    void findAll_returnsPagedSubscriptions() {
        repository.save(Subscription.create("One", BRL, Money.of("10.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));
        repository.save(Subscription.create("Two", BRL, Money.of("20.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));
        repository.save(Subscription.create("Three", BRL, Money.of("30.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "cc-test"));

        PageResult<Subscription> result = repository.findAll(0, 2);

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }
}
