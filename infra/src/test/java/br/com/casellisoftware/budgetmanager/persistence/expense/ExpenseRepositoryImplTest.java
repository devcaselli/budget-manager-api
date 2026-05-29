package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(ExpenseRepositoryImpl.class)
@ComponentScan(basePackageClasses = ExpensePersistenceMapper.class)
class ExpenseRepositoryImplTest extends AbstractMongoIntegrationTest {

    @Autowired
    private ExpenseRepositoryImpl repository;

    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(1);
    private static final String CREDIT_CARD_ID = "cc-1";

    private static Expense newExpense(String name, String amount, String walletId) {
        return newExpense(name, amount, walletId, Expense.LEGACY_OWNER_ID);
    }

    private static Expense newExpense(String name, String amount, String walletId, String ownerId) {
        return Expense.create(walletId, CREDIT_CARD_ID, name, Money.of(amount), PURCHASE_DATE, null, false, null, ownerId);
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
        assertThat(saved.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
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
    void existsById_returnsWhetherExpenseExists() {
        Expense saved = repository.save(newExpense("coffee", "5.00", "wallet-1"));

        assertThat(repository.existsById(saved.getId())).isTrue();
        assertThat(repository.existsById("nonexistent-id")).isFalse();
    }

    @Test
    void existsAnyByCreditCardId_returnsTrue_whenAtLeastOneExpenseExists() {
        repository.save(Expense.create("wallet-1", "cc-1", "Coffee", Money.of("5.00"), LocalDate.now(), null));

        assertThat(repository.existsAnyByCreditCardId("cc-1")).isTrue();
    }

    @Test
    void existsAnyByCreditCardId_returnsFalse_whenNoExpenseWithCard() {
        assertThat(repository.existsAnyByCreditCardId("cc-ghost")).isFalse();
    }

    @Test
    void findByWalletId_returnsPagedResults() {
        repository.save(newExpense("lunch", "10.50", "wallet-1"));
        repository.save(newExpense("coffee", "5.00", "wallet-1"));
        repository.save(newExpense("dinner", "30.00", "wallet-1"));
        repository.save(newExpense("other-wallet-expense", "15.00", "wallet-2"));

        PageResult<Expense> result = repository.findByWalletId("wallet-1", 0, 10, false);

        assertThat(result.content()).hasSize(3);
        assertThat(result.content()).allMatch(e -> e.getWalletId().equals("wallet-1"));
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void findByWalletId_respectsPageSize() {
        repository.save(newExpense("lunch", "10.50", "wallet-1"));
        repository.save(newExpense("coffee", "5.00", "wallet-1"));
        repository.save(newExpense("dinner", "30.00", "wallet-1"));

        PageResult<Expense> firstPage = repository.findByWalletId("wallet-1", 0, 2, false);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);

        PageResult<Expense> secondPage = repository.findByWalletId("wallet-1", 1, 2, false);

        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.page()).isEqualTo(1);
    }

    @Test
    void findByWalletId_noExpenses_returnsEmptyPage() {
        PageResult<Expense> result = repository.findByWalletId("nonexistent-wallet", 0, 10, false);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void findIdsByCreditCardId_returnsAllMatchingIds() {
        Expense e1 = repository.save(Expense.create("wallet-1", "cc-1", "Lunch", Money.of("10.00"), LocalDate.now(), null));
        Expense e2 = repository.save(Expense.create("wallet-1", "cc-1", "Dinner", Money.of("20.00"), LocalDate.now(), null));
        repository.save(Expense.create("wallet-1", "cc-2", "Other", Money.of("5.00"), LocalDate.now(), null));

        List<String> ids = repository.findIdsByCreditCardId("cc-1");

        assertThat(ids).containsExactlyInAnyOrder(e1.getId(), e2.getId());
    }

    @Test
    void findIdsByCreditCardId_returnsEmpty_whenNoMatch() {
        assertThat(repository.findIdsByCreditCardId("cc-ghost")).isEmpty();
    }

    @Test
    void findByCreditCardId_filtersByWalletIdsAndName_andReturnsTotalCost() {
        Expense fuel = repository.save(Expense.create("wallet-may", "cc-1", "Fuel", Money.of("100.00"), LocalDate.now(), null));
        Expense fuelExtra = repository.save(Expense.create("wallet-june", "cc-1", "Fuel Extra", Money.of("50.00"), LocalDate.now(), null));
        repository.save(Expense.create("wallet-june", "cc-1", "Market", Money.of("70.00"), LocalDate.now(), null));
        repository.save(Expense.create("wallet-may", "cc-2", "Fuel", Money.of("999.00"), LocalDate.now(), null));

        var result = repository.findByCreditCardId(
                "cc-1",
                new br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardFilter(
                        List.of("wallet-may", "wallet-june"),
                        "fuel"
                ),
                0,
                10
        );

        assertThat(result.expenses().content())
                .extracting(Expense::getId)
                .containsExactlyInAnyOrder(fuel.getId(), fuelExtra.getId());
        assertThat(result.expenses().totalElements()).isEqualTo(2);
        assertThat(result.totalCost()).isEqualByComparingTo("150.00");
    }

    @Test
    void findByCreditCardId_ignoresHiddenExpensesInContentAndTotalCost() {
        Expense visible = repository.save(Expense.create("wallet-may", "cc-1", "Visible Fuel", Money.of("100.00"), LocalDate.now(), null));
        repository.save(Expense.create("wallet-may", "cc-1", "Hidden Source", Money.of("600.00"), LocalDate.now(), null, true));

        var result = repository.findByCreditCardId(
                "cc-1",
                new br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardFilter(
                        List.of("wallet-may"),
                        null
                ),
                0,
                10
        );

        assertThat(result.expenses().content()).extracting(Expense::getId).containsExactly(visible.getId());
        assertThat(result.expenses().totalElements()).isEqualTo(1);
        assertThat(result.totalCost()).isEqualByComparingTo("100.00");
    }

    @Test
    void findByWalletId_hidesHiddenExpensesByDefault_andIncludesThemWhenUnhidden() {
        Expense hidden = repository.save(Expense.create("wallet-1", "cc-1", "Installment Source", Money.of("6000.00"), LocalDate.now(), null, true));
        Expense visible = repository.save(Expense.create("wallet-1", "cc-1", "Installment Current", Money.of("1000.00"), LocalDate.now(), null));

        PageResult<Expense> defaultResult = repository.findByWalletId("wallet-1", 0, 10, false);
        PageResult<Expense> unhiddenResult = repository.findByWalletId("wallet-1", 0, 10, true);

        assertThat(defaultResult.content()).extracting(Expense::getId).containsExactly(visible.getId());
        assertThat(unhiddenResult.content()).extracting(Expense::getId).containsExactlyInAnyOrder(hidden.getId(), visible.getId());
    }

    @Test
    void deleteById_removesExpenseOnlyForOwner() {
        Expense saved = repository.save(newExpense("coffee", "5.00", "wallet-1", "owner-1"));

        assertThatThrownBy(() -> repository.deleteById(saved.getId(), "owner-2"))
                .isInstanceOf(ExpenseNotFoundException.class);
        assertThat(repository.findById(saved.getId())).isPresent();

        repository.deleteById(saved.getId(), "owner-1");

        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
