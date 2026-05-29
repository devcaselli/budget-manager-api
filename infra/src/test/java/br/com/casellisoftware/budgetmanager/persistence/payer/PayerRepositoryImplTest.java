package br.com.casellisoftware.budgetmanager.persistence.payer;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.persistence.payer.mappers.PayerPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@Import(PayerRepositoryImpl.class)
@ComponentScan(basePackageClasses = PayerPersistenceMapper.class)
class PayerRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private PayerRepositoryImpl repository;

    @Test
    void save_thenFindById_returnsMappedDomain() {
        Payer saved = repository.save(payer("Joao", false));

        assertThat(repository.findById(saved.getId(), "owner-1"))
                .isPresent()
                .get()
                .extracting(Payer::getName)
                .isEqualTo("Joao");
    }

    @Test
    void findAll_returnsOnlyOwnerNonDeletedPayers() {
        Payer visible = repository.save(payer("Visible", false));
        repository.save(payer("Deleted", true));
        repository.save(new Payer(
                "payer-other-owner",
                "owner-2",
                "Other",
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                false));

        assertThat(repository.findAll("owner-1"))
                .extracting(Payer::getId)
                .containsExactly(visible.getId());
    }

    @Test
    void deleteById_marksDeletedWithoutRemovingDocument() {
        Payer saved = repository.save(payer("Joao", false));

        repository.deleteById(saved.getId(), "owner-1");

        assertThat(repository.findById(saved.getId(), "owner-1")).isPresent();
        assertThat(repository.findById(saved.getId(), "owner-1").get().isDeleted()).isTrue();
        assertThat(repository.findAll("owner-1")).isEmpty();
    }

    @Test
    void findAllStandingAndFindAllByWalletId_filterByLifecycleAndWallet() {
        Payer standing = repository.save(payer("Standing", false));
        Payer transientPayer = repository.save(new Payer(
                "payer-transient",
                "owner-1",
                "Transient",
                PayerType.TRANSIENT,
                "wallet-1",
                null,
                LocalDate.of(2026, 5, 10),
                false));

        assertThat(repository.findAllStanding("owner-1"))
                .extracting(Payer::getId)
                .containsExactly(standing.getId());
        assertThat(repository.findAllByWalletId("wallet-1", "owner-1"))
                .extracting(Payer::getId)
                .containsExactly(transientPayer.getId());
    }

    private static Payer payer(String name, boolean deleted) {
        return new Payer(
                "payer-" + name,
                "owner-1",
                name,
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                deleted);
    }
}
