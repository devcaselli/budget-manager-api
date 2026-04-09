package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({ExpenseRepositoryImpl.class, ExpensePersistenceMapper.class})
class ExpenseRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ExpenseRepositoryImpl repository;

    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(1);

    private static Expense newExpense(String name, String amount, String walletId) {
        return Expense.create(walletId, name, Money.of(amount), PURCHASE_DATE);
    }

    @Test
    void save_persistsAllFields() {
        Expense saved = repository.save(newExpense("lunch", "10.50", "wallet-1"));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getName()).isEqualTo("lunch");
        assertThat(saved.getCost().amount()).isEqualByComparingTo("10.50");
        assertThat(saved.getRemaining().amount()).isEqualByComparingTo("10.50");
        assertThat(saved.getPurchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void findById_whenFound_returnsMappedExpense() {
        Expense saved = repository.save(newExpense("coffee", "5.00", "wallet-1"));

        Optional<Expense> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("coffee");
        assertThat(result.get().getCost().amount()).isEqualByComparingTo("5.00");
        assertThat(result.get().getPurchaseDate()).isEqualTo(PURCHASE_DATE);
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("nonexistent-id")).isEmpty();
    }

    @Test
    void findAllByWalletId_returnsOnlyMatchingWallet() {
        repository.save(newExpense("lunch", "10", "wallet-1"));
        repository.save(newExpense("dinner", "20", "wallet-1"));
        repository.save(newExpense("other", "5", "wallet-2"));

        List<Expense> result = repository.findAllByWalletId("wallet-1");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Expense::getName).containsExactlyInAnyOrder("lunch", "dinner");
    }

    @Test
    void update_happyPath_persistsUpdatedFields() {
        Expense saved = repository.save(newExpense("old name", "10", "wallet-1"));
        Expense updatedData = newExpense("new name", "99.99", "wallet-1");

        Expense result = repository.update(updatedData, saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getName()).isEqualTo("new name");
        assertThat(result.getCost().amount()).isEqualByComparingTo("99.99");
    }

    @Test
    void update_notFound_throwsExpenseNotFoundException() {
        Expense expense = newExpense("x", "1", "w");

        assertThatThrownBy(() -> repository.update(expense, "nonexistent-id"))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void update_nullId_throwsIllegalArgument() {
        Expense expense = newExpense("x", "1", "w");

        assertThatThrownBy(() -> repository.update(expense, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_happyPath_removesExpense() {
        Expense saved = repository.save(newExpense("lunch", "10", "wallet-1"));

        repository.delete(saved);

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void delete_notFound_throwsExpenseNotFoundException() {
        // rehydrate lets us build a valid Expense with an id that doesn't exist in Mongo.
        Expense expense = Expense.rehydrate(
                "nonexistent-id", "w", "x", Money.of("1"), Money.of("1"), PURCHASE_DATE);

        assertThatThrownBy(() -> repository.delete(expense))
                .isInstanceOf(ExpenseNotFoundException.class);
    }

    @Test
    void delete_nullExpense_throwsIllegalArgument() {
        assertThatThrownBy(() -> repository.delete(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
