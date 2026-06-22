package br.com.casellisoftware.budgetmanager.persistence.reservedbudget;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.configs.MongoConfiguration;
import br.com.casellisoftware.budgetmanager.configs.SharedBeanConfiguration;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.reservedbudget.mappers.ReservedBudgetPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ReservedBudgetRepositoryImpl} using Testcontainers MongoDB.
 *
 * <p>Focuses on the Vínculos feature: link persistence round-trip and
 * {@code findByLinkedSource} cardinality query.</p>
 */
@Import({ReservedBudgetRepositoryImpl.class, MongoConfiguration.class, SharedBeanConfiguration.class})
@ComponentScan(basePackageClasses = ReservedBudgetPersistenceMapper.class)
class ReservedBudgetRepositoryImplTest extends AbstractMongoIntegrationTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String OWNER = "owner-1";
    private static final String OTHER_OWNER = "owner-2";
    private static final YearMonth START = YearMonth.of(2025, 1);
    private static final YearMonth FROM = YearMonth.of(2025, 6);

    @Autowired
    private ReservedBudgetRepositoryImpl repository;

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ReservedBudget saveRb(String name, String ownerId) {
        return repository.save(
                ReservedBudget.create(name, null, BRL, Money.of("2000.00", BRL), START, FlagEnum.NONE, ownerId));
    }

    private static ReservedBudgetLink subLink(String subId) {
        return new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, subId, FROM);
    }

    private static ReservedBudgetLink instLink(String instId) {
        return new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, instId, FROM);
    }

    // ─── basic round-trip ────────────────────────────────────────────────────

    @Test
    void save_withLinks_thenFindById_linksHydrated() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        rb = repository.save(rb.addLink(subLink("sub-1")));

        Optional<ReservedBudget> found = repository.findById(rb.getId(), OWNER);

        assertThat(found).isPresent();
        assertThat(found.get().getLinks()).hasSize(1);
        ReservedBudgetLink link = found.get().getLinks().get(0);
        assertThat(link.sourceType()).isEqualTo(ReservedBudgetLinkSourceType.SUBSCRIPTION);
        assertThat(link.sourceId()).isEqualTo("sub-1");
        assertThat(link.fromMonth()).isEqualTo(FROM);
    }

    @Test
    void save_noLinks_thenFindById_linksEmpty() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);

        Optional<ReservedBudget> found = repository.findById(rb.getId(), OWNER);

        assertThat(found).isPresent();
        assertThat(found.get().getLinks()).isEmpty();
    }

    @Test
    void save_multipleLinks_allPersistedAndHydrated() {
        ReservedBudget rb = saveRb("Casa", OWNER);
        rb = repository.save(rb
                .addLink(subLink("sub-1"))
                .addLink(instLink("inst-1")));

        Optional<ReservedBudget> found = repository.findById(rb.getId(), OWNER);

        assertThat(found).isPresent();
        assertThat(found.get().getLinks()).hasSize(2);
    }

    // ─── findByLinkedSource ───────────────────────────────────────────────────

    @Test
    void findByLinkedSource_subscription_returnsOwnerRb() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        repository.save(rb.addLink(subLink("sub-netflix")));

        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix", OWNER);

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("Aluguel");
    }

    @Test
    void findByLinkedSource_installment_returnsOwnerRb() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        repository.save(rb.addLink(instLink("inst-notebook")));

        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.INSTALLMENT, "inst-notebook", OWNER);

        assertThat(found).isPresent();
    }

    @Test
    void findByLinkedSource_notLinked_returnsEmpty() {
        saveRb("Aluguel", OWNER); // no links

        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-unknown", OWNER);

        assertThat(found).isEmpty();
    }

    @Test
    void findByLinkedSource_wrongOwner_returnsEmpty() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        repository.save(rb.addLink(subLink("sub-netflix")));

        // same sourceId but different owner → should not see it
        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix", OTHER_OWNER);

        assertThat(found).isEmpty();
    }

    @Test
    void findByLinkedSource_wrongType_returnsEmpty() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        repository.save(rb.addLink(subLink("sub-netflix")));

        // sourceId matches but wrong type
        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.INSTALLMENT, "sub-netflix", OWNER);

        assertThat(found).isEmpty();
    }

    @Test
    void findByLinkedSource_deletedRb_returnsEmpty() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        rb = repository.save(rb.addLink(subLink("sub-netflix")));
        // logical delete
        repository.save(rb.markDeleted(LocalDateTime.now()));

        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix", OWNER);

        assertThat(found).isEmpty();
    }

    @Test
    void findByLinkedSource_multipleRbs_returnsCorrectOne() {
        ReservedBudget rb1 = saveRb("RB-1", OWNER);
        ReservedBudget rb2 = saveRb("RB-2", OWNER);
        repository.save(rb1.addLink(subLink("sub-netflix")));
        repository.save(rb2.addLink(subLink("sub-spotify")));

        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-spotify", OWNER);

        assertThat(found).isPresent();
        assertThat(found.get().getDescription()).isEqualTo("RB-2");
    }

    @Test
    void removeLink_thenFindByLinkedSource_returnsEmpty() {
        ReservedBudget rb = saveRb("Aluguel", OWNER);
        rb = repository.save(rb.addLink(subLink("sub-netflix")));

        // unlink
        repository.save(rb.removeLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix"));

        Optional<ReservedBudget> found = repository.findByLinkedSource(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix", OWNER);

        assertThat(found).isEmpty();
    }
}
