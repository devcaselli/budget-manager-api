package br.com.casellisoftware.budgetmanager.persistence.creditcard;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.creditcard.mappers.CreditCardPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link CreditCardRepositoryImpl}: exercises the real
 * MapStruct mapper and a real Mongo container to verify the dumb-adapter
 * contract (persist / read / paginate / delete).
 */
@Import(CreditCardRepositoryImpl.class)
@ComponentScan(basePackageClasses = CreditCardPersistenceMapper.class)
class CreditCardRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private CreditCardRepositoryImpl repository;

    @Test
    void save_newCard_persistsIdAndName() {
        CreditCard saved = repository.save(CreditCard.create("Nubank"));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getName()).isEqualTo("Nubank");
    }

    @Test
    void save_thenFindById_returnsMappedDomain() {
        CreditCard saved = repository.save(CreditCard.create("Itaú"));

        Optional<CreditCard> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getName()).isEqualTo("Itaú");
    }

    @Test
    void findById_missing_returnsEmpty() {
        assertThat(repository.findById("nonexistent-id")).isEmpty();
    }

    @Test
    void existsById_returnsWhetherCardExists() {
        CreditCard saved = repository.save(CreditCard.create("Nubank"));

        assertThat(repository.existsById(saved.getId())).isTrue();
        assertThat(repository.existsById("nonexistent-id")).isFalse();
    }

    @Test
    void findAll_returnsPagedResults() {
        repository.save(CreditCard.create("Nubank"));
        repository.save(CreditCard.create("Itaú"));
        repository.save(CreditCard.create("Bradesco"));

        PageResult<CreditCard> result = repository.findAll(0, 10);

        assertThat(result.content()).hasSize(3);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void findAll_respectsPageSize() {
        repository.save(CreditCard.create("Nubank"));
        repository.save(CreditCard.create("Itaú"));
        repository.save(CreditCard.create("Bradesco"));

        PageResult<CreditCard> firstPage = repository.findAll(0, 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        PageResult<CreditCard> secondPage = repository.findAll(1, 2);

        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.page()).isEqualTo(1);
    }

    @Test
    void findAll_empty_returnsEmptyPage() {
        PageResult<CreditCard> result = repository.findAll(0, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void deleteById_removesCard() {
        CreditCard saved = repository.save(CreditCard.create("Nubank"));

        repository.deleteById(saved.getId());

        assertThat(repository.findById(saved.getId())).isEmpty();
        assertThat(repository.existsById(saved.getId())).isFalse();
    }

    @Test
    void save_updateExistingCard_preservesIdAndIncrementsVersion() {
        CreditCard first = repository.save(CreditCard.create("Nubank"));

        // Same id, different field: reconstruct via public ctor.
        CreditCard renamed = new CreditCard(first.getId(), "Nubank Ultravioleta");
        CreditCard second = repository.save(renamed);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getName()).isEqualTo("Nubank Ultravioleta");

        Optional<CreditCard> reloaded = repository.findById(first.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getName()).isEqualTo("Nubank Ultravioleta");
    }
}
