package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseRepositoryImplTest {

    @Mock
    private ExpenseMongoRepository mongoRepository;

    @Mock
    private ExpensePersistenceMapper mapper;

    private ExpenseRepositoryImpl repository;

    private Expense domain;
    private ExpenseDocument document;

    @BeforeEach
    void setUp() {
        repository = new ExpenseRepositoryImpl(mongoRepository, mapper);
        domain = new Expense("id-1", "lunch", new BigDecimal("10"), Instant.parse("2026-04-07T12:00:00Z"), null, "wallet-1");
        document = new ExpenseDocument("id-1", "lunch", new BigDecimal("10"), Instant.parse("2026-04-07T12:00:00Z"), null, "wallet-1");
    }

    @Test
    void findAllByWalletId_mapsEachDocument() {
        when(mongoRepository.findAllByWalletId("wallet-1")).thenReturn(List.of(document));
        when(mapper.expenseDocumentToExpense(document)).thenReturn(domain);

        List<Expense> result = repository.findAllByWalletId("wallet-1");

        assertThat(result).containsExactly(domain);
    }

    @Test
    void findById_whenFound_returnsMappedExpense() {
        when(mongoRepository.findById("id-1")).thenReturn(Optional.of(document));
        when(mapper.expenseDocumentToExpense(document)).thenReturn(domain);

        Optional<Expense> result = repository.findById("id-1");

        assertThat(result).contains(domain);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        when(mongoRepository.findById("missing")).thenReturn(Optional.empty());

        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void save_mapsPersistsAndReturnsDomain() {
        when(mapper.expenseDomainToExpenseDocument(domain)).thenReturn(document);
        when(mongoRepository.save(document)).thenReturn(document);
        when(mapper.expenseDocumentToExpense(document)).thenReturn(domain);

        Expense result = repository.save(domain);

        assertThat(result).isSameAs(domain);
        verify(mongoRepository).save(document);
    }

    @Test
    void delete_happyPath_deletesById() {
        when(mongoRepository.existsById("id-1")).thenReturn(true);

        repository.delete(domain);

        verify(mongoRepository).deleteById("id-1");
    }

    @Test
    void delete_nullExpense_throwsIllegalArgument() {
        assertThatThrownBy(() -> repository.delete(null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mongoRepository, never()).deleteById(any());
    }

    @Test
    void delete_nullId_throwsIllegalArgument() {
        Expense noId = new Expense(null, "x", BigDecimal.ONE, Instant.now(), null, "w");
        assertThatThrownBy(() -> repository.delete(noId))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mongoRepository, never()).deleteById(any());
    }

    @Test
    void delete_notFound_throwsExpenseNotFound() {
        when(mongoRepository.existsById("id-1")).thenReturn(false);

        assertThatThrownBy(() -> repository.delete(domain))
                .isInstanceOf(ExpenseNotFoundException.class);
        verify(mongoRepository, never()).deleteById(any());
    }

    @Test
    void update_happyPath_doesNotMutateInputAndSetsIdOnDocument() {
        Expense input = new Expense(null, "lunch", new BigDecimal("10"), Instant.parse("2026-04-07T12:00:00Z"), null, "wallet-1");
        ExpenseDocument freshDoc = new ExpenseDocument(null, "lunch", new BigDecimal("10"), Instant.parse("2026-04-07T12:00:00Z"), null, "wallet-1");

        when(mongoRepository.existsById("id-1")).thenReturn(true);
        when(mapper.expenseDomainToExpenseDocument(input)).thenReturn(freshDoc);
        when(mongoRepository.save(any(ExpenseDocument.class))).thenReturn(document);
        when(mapper.expenseDocumentToExpense(document)).thenReturn(domain);

        Expense result = repository.update(input, "id-1");

        assertThat(result).isSameAs(domain);
        assertThat(input.getId()).as("input must not be mutated").isNull();

        ArgumentCaptor<ExpenseDocument> captor = ArgumentCaptor.forClass(ExpenseDocument.class);
        verify(mongoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("id-1");
    }

    @Test
    void update_nullId_throwsIllegalArgument() {
        assertThatThrownBy(() -> repository.update(domain, null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mongoRepository, never()).save(any());
    }

    @Test
    void update_notFound_throwsExpenseNotFound() {
        when(mongoRepository.existsById("id-1")).thenReturn(false);

        assertThatThrownBy(() -> repository.update(domain, "id-1"))
                .isInstanceOf(ExpenseNotFoundException.class);
        verify(mongoRepository, never()).save(any());
    }
}
