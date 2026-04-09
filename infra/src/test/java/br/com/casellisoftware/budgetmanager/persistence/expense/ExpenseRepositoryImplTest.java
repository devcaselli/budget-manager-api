package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapperImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({ExpenseRepositoryImpl.class, ExpensePersistenceMapperImpl.class})
class ExpenseRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ExpenseRepositoryImpl repository;

    private static final Instant PURCHASE_DATE = Instant.parse("2026-04-07T12:00:00Z");

    @Test
    void save_persistsAllFields() {
        Expense expense = new Expense(null, "lunch", new BigDecimal("10.50"), PURCHASE_DATE, null, "wallet-1");

        Expense saved = repository.save(expense);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("lunch");
        assertThat(saved.getCost()).isEqualByComparingTo(new BigDecimal("10.50"));
        assertThat(saved.getPurchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(saved.getRemaining()).isNull();
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void findById_whenFound_returnsMappedExpense() {
        Expense saved = repository.save(new Expense(null, "coffee", new BigDecimal("5.00"), PURCHASE_DATE, null, "wallet-1"));

        Optional<Expense> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("coffee");
        assertThat(result.get().getCost()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(result.get().getPurchaseDate()).isEqualTo(PURCHASE_DATE);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("nonexistent-id")).isEmpty();
    }

    @Test
    void findAllByWalletId_returnsOnlyMatchingWallet() {
        repository.save(new Expense(null, "lunch", new BigDecimal("10"), PURCHASE_DATE, null, "wallet-1"));
        repository.save(new Expense(null, "dinner", new BigDecimal("20"), PURCHASE_DATE, null, "wallet-1"));
        repository.save(new Expense(null, "other", new BigDecimal("5"), PURCHASE_DATE, null, "wallet-2"));

        List<Expense> result = repository.findAllByWalletId("wallet-1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Expense::getName).containsExactlyInAnyOrder("lunch", "dinner");
    }

    @Test
    void update_happyPath_persistsUpdatedFields() {
        Expense saved = repository.save(new Expense(null, "old name", new BigDecimal("10"), PURCHASE_DATE, null, "wallet-1"));
        Expense updatedData = new Expense(null, "new name", new BigDecimal("99.99"), PURCHASE_DATE, null, "wallet-1");

        Expense result = repository.update(updatedData, saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo("new name");
        assertThat(result.getCost()).isEqualByComparingTo(new BigDecimal("99.99"));
        assertThat(updatedData.getId()).as("input must not be mutated").isNull();
    }

    @Test
    void update_notFound_throwsExpenseNotFoundException() {
        Expense expense = new Expense(null, "x", BigDecimal.ONE, PURCHASE_DATE, null, "w");

        assertThatThrownBy(() -> repository.update(expense, "nonexistent-id"))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void update_nullId_throwsIllegalArgument() {
        Expense expense = new Expense(null, "x", BigDecimal.ONE, PURCHASE_DATE, null, "w");

        assertThatThrownBy(() -> repository.update(expense, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_happyPath_removesExpense() {
        Expense saved = repository.save(new Expense(null, "lunch", new BigDecimal("10"), PURCHASE_DATE, null, "wallet-1"));

        repository.delete(saved);

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_notFound_throwsExpenseNotFoundException() {
        Expense expense = new Expense("nonexistent-id", "x", BigDecimal.ONE, PURCHASE_DATE, null, "w");

        assertThatThrownBy(() -> repository.delete(expense))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void delete_nullExpense_throwsIllegalArgument() {
        assertThatThrownBy(() -> repository.delete(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_nullId_throwsIllegalArgument() {
        Expense expense = new Expense(null, "x", BigDecimal.ONE, PURCHASE_DATE, null, "w");

        assertThatThrownBy(() -> repository.delete(expense))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
