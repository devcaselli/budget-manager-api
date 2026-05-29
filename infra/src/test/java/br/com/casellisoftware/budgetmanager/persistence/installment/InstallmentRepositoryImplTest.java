package br.com.casellisoftware.budgetmanager.persistence.installment;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.configs.MongoConfiguration;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.installment.mappers.InstallmentPersistenceMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({InstallmentRepositoryImpl.class, MongoConfiguration.class})
@ComponentScan(basePackageClasses = InstallmentPersistenceMapper.class)
class InstallmentRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private InstallmentRepositoryImpl repository;

    @Autowired
    private MongoOperations mongoOperations;

    private static Installment installment(String description,
                                           String creditCardId,
                                           String sourceWalletId,
                                           YearMonth sourceMonth,
                                           int installmentNumber,
                                           String originalAmount,
                                           String installmentAmount) {
        return Installment.create(
                description,
                Money.of(new BigDecimal(originalAmount)),
                Money.of(new BigDecimal(installmentAmount)),
                installmentNumber,
                LocalDate.of(2026, 5, 10),
                creditCardId,
                sourceWalletId,
                sourceMonth,
                FlagEnum.NONE
        );
    }

    @Test
    void save_persistsFlattenedMoneyAndYearMonthFields() {
        Installment saved = repository.save(installment(
                "Notebook", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));

        Document document = mongoOperations
                .getCollection("installmentDocument")
                .find(new Document("_id", saved.getId()))
                .first();

        assertThat(document).isNotNull();
        assertThat(document.get("description")).isEqualTo("Notebook");
        assertThat(document.get("originalAmount").toString()).isEqualTo("6000.00");
        assertThat(document.get("installmentAmount").toString()).isEqualTo("1000.00");
        assertThat(document.get("currency")).isEqualTo("BRL");
        assertThat(document.get("installmentNumber")).isEqualTo(6);
        assertThat(document.get("lastInstallmentDate")).isEqualTo("2026-10");
        assertThat(document.get("creditCardId")).isEqualTo("cc1");
        assertThat(document.get("sourceWalletId")).isEqualTo("w1");
        assertThat(document.get("sourceEffectiveMonth")).isEqualTo("2026-05");
        assertThat(document.get("deleted")).isEqualTo(false);
    }

    @Test
    void findById_whenFound_returnsMappedInstallment() {
        Installment saved = repository.save(installment(
                "Notebook", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));

        Optional<Installment> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("Notebook");
        assertThat(result.get().getCreditCardId()).isEqualTo("cc1");
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void existsById_returnsTrue_whenFound() {
        Installment saved = repository.save(installment(
                "X", "cc1", "w1", YearMonth.of(2026, 5), 6, "600.00", "100.00"));

        assertThat(repository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_returnsFalse_whenMissing() {
        assertThat(repository.existsById("ghost")).isFalse();
    }

    @Test
    void findActiveAffecting_returnsOnlyActiveWindowMatches() {
        Installment affecting = repository.save(installment(
                "Notebook", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));
        Installment sourceMonth = repository.save(installment(
                "Phone", "cc1", "w2", YearMonth.of(2026, 6), 6, "3000.00", "500.00"));
        Installment expired = repository.save(installment(
                "Old", "cc2", "w3", YearMonth.of(2026, 1), 2, "1000.00", "500.00"));
        Installment deleted = repository.save(installment(
                "Deleted", "cc3", "w4", YearMonth.of(2026, 5), 6, "6000.00", "1000.00")
                .delete(java.time.Clock.systemUTC()));

        List<Installment> result = repository.findActiveAffecting(YearMonth.of(2026, 6));

        assertThat(result)
                .extracting(Installment::getId)
                .contains(affecting.getId())
                .doesNotContain(sourceMonth.getId(), expired.getId(), deleted.getId());
    }

    @Test
    void findBySourceWalletIdAndNotDeleted_returnsOnlyActiveFromWallet() {
        Installment active = repository.save(installment(
                "Notebook", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));
        Installment deleted = repository.save(installment(
                "Deleted", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00")
                .delete(java.time.Clock.systemUTC()));
        Installment otherWallet = repository.save(installment(
                "Other", "cc1", "w2", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));

        List<Installment> result = repository.findBySourceWalletIdAndNotDeleted("w1");

        assertThat(result)
                .extracting(Installment::getId)
                .containsExactly(active.getId())
                .doesNotContain(deleted.getId(), otherWallet.getId());
    }

    @Test
    void findIdsByCreditCardIdAndNotDeleted_returnsOnlyActiveIds() {
        Installment active1 = repository.save(installment(
                "A", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));
        Installment active2 = repository.save(installment(
                "B", "cc1", "w2", YearMonth.of(2026, 5), 6, "3000.00", "500.00"));
        Installment deleted = repository.save(installment(
                "Deleted", "cc1", "w3", YearMonth.of(2026, 5), 6, "6000.00", "1000.00")
                .delete(java.time.Clock.systemUTC()));
        repository.save(installment(
                "Other Card", "cc2", "w4", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));

        List<String> ids = repository.findIdsByCreditCardIdAndNotDeleted("cc1");

        assertThat(ids)
                .containsExactlyInAnyOrder(active1.getId(), active2.getId())
                .doesNotContain(deleted.getId());
    }

    @Test
    void findIdsByCreditCardId_includesDeletedIdsForHistoricalReferenceChecks() {
        Installment active = repository.save(installment(
                "A", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));
        Installment deleted = repository.save(installment(
                "Deleted", "cc1", "w3", YearMonth.of(2026, 5), 6, "6000.00", "1000.00")
                .delete(java.time.Clock.systemUTC()));
        repository.save(installment(
                "Other Card", "cc2", "w4", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));

        List<String> ids = repository.findIdsByCreditCardId("cc1");

        assertThat(ids).containsExactlyInAnyOrder(active.getId(), deleted.getId());
    }

    @Test
    void findAll_returnsPagedResults() {
        repository.save(installment(
                "A", "cc1", "w1", YearMonth.of(2026, 5), 6, "6000.00", "1000.00"));
        repository.save(installment(
                "B", "cc2", "w2", YearMonth.of(2026, 5), 6, "3000.00", "500.00"));

        PageResult<Installment> result = repository.findAll(0, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void findAll_secondPageReturnsRemainder() {
        repository.save(installment(
                "A", "cc1", "w1", YearMonth.of(2026, 5), 6, "600.00", "100.00"));
        repository.save(installment(
                "B", "cc2", "w2", YearMonth.of(2026, 5), 6, "300.00", "50.00"));

        PageResult<Installment> firstPage = repository.findAll(0, 1);
        PageResult<Installment> secondPage = repository.findAll(1, 1);

        assertThat(firstPage.content()).hasSize(1);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).hasSize(1);
    }
}
