package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
}
