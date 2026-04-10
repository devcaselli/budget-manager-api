package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final Money TEN = Money.of("10.00");

    // ---- create: happy path ----

    @Test
    void create_happyPath_generatesIdAndInitializesRemaining() {
        Expense expense = Expense.create("wallet-1", "lunch", TEN, YESTERDAY);

        assertThat(expense.getId()).isNotBlank();
        assertThat(expense.getWalletId()).isEqualTo("wallet-1");
        assertThat(expense.getName()).isEqualTo("lunch");
        assertThat(expense.getCost()).isEqualTo(TEN);
        assertThat(expense.getRemaining()).isEqualTo(TEN);
        assertThat(expense.getPurchaseDate()).isEqualTo(YESTERDAY);
    }

    // ---- create: business rules ----

    @Test
    void create_rejectsZeroCost() {
        assertThatThrownBy(() -> Expense.create("w1", "lunch", Money.zero(), TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cost");
    }

    @Test
    void create_rejectsFuturePurchaseDate() {
        assertThatThrownBy(() -> Expense.create("w1", "lunch", TEN, TODAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    // ---- debit ----

    @Test
    void debit_reducesRemaining() {
        Expense expense = Expense.create("w1", "lunch", TEN, TODAY);

        Expense after = expense.debit(Money.of("4.00"));

        assertThat(after.getRemaining()).isEqualTo(Money.of("6.00"));
        assertThat(after.getCost()).isEqualTo(TEN);
        assertThat(after.getId()).isEqualTo(expense.getId());
        // original is unchanged (immutability)
        assertThat(expense.getRemaining()).isEqualTo(TEN);
    }

    @Test
    void debit_rejectsAmountExceedingRemaining() {
        Expense expense = Expense.create("w1", "lunch", TEN, TODAY);
        assertThatThrownBy(() -> expense.debit(Money.of("10.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds remaining");
    }

    @Test
    void debit_rejectsNonPositiveAmount() {
        Expense expense = Expense.create("w1", "lunch", TEN, TODAY);
        assertThatThrownBy(() -> expense.debit(Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    // ---- constructor (used by MapStruct for reconstruction) ----

    @Test
    void constructor_allowsPartiallyPaidState() {
        Expense expense = new Expense("id-1", "w1", "lunch", TEN, Money.of("3.00"), YESTERDAY);

        assertThat(expense.getRemaining()).isEqualTo(Money.of("3.00"));
        assertThat(expense.getCost()).isEqualTo(TEN);
    }

    @Test
    void constructor_allowsFuturePurchaseDate() {
        // create() rejects future dates; constructor does not — historical data wins.
        Expense expense = new Expense("id-1", "w1", "lunch", TEN, TEN, TODAY.plusDays(30));

        assertThat(expense.getPurchaseDate()).isEqualTo(TODAY.plusDays(30));
    }
}
