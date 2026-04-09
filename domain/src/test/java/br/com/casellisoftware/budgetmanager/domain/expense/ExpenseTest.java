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

    @Test
    void create_rejectsNullWalletId() {
        assertThatThrownBy(() -> Expense.create(null, "lunch", TEN, TODAY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void create_rejectsBlankWalletId() {
        assertThatThrownBy(() -> Expense.create("  ", "lunch", TEN, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void create_rejectsNullName() {
        assertThatThrownBy(() -> Expense.create("w1", null, TEN, TODAY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsBlankName() {
        assertThatThrownBy(() -> Expense.create("w1", "  ", TEN, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsNameExceedingMaxLength() {
        String tooLong = "x".repeat(Expense.MAX_NAME_LENGTH + 1);
        assertThatThrownBy(() -> Expense.create("w1", tooLong, TEN, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length");
    }

    @Test
    void create_rejectsNullCost() {
        assertThatThrownBy(() -> Expense.create("w1", "lunch", null, TODAY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cost");
    }

    @Test
    void create_rejectsZeroCost() {
        assertThatThrownBy(() -> Expense.create("w1", "lunch", Money.zero(), TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cost");
    }

    @Test
    void create_rejectsNullPurchaseDate() {
        assertThatThrownBy(() -> Expense.create("w1", "lunch", TEN, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("purchaseDate");
    }

    @Test
    void create_rejectsFuturePurchaseDate() {
        assertThatThrownBy(() -> Expense.create("w1", "lunch", TEN, TODAY.plusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

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

    @Test
    void debit_rejectsNullAmount() {
        Expense expense = Expense.create("w1", "lunch", TEN, TODAY);
        assertThatThrownBy(() -> expense.debit(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rehydrate_acceptsPartiallyPaidState() {
        Expense expense = Expense.rehydrate(
                "id-1", "w1", "lunch", TEN, Money.of("3.00"), YESTERDAY);

        assertThat(expense.getRemaining()).isEqualTo(Money.of("3.00"));
        assertThat(expense.getCost()).isEqualTo(TEN);
    }

    @Test
    void rehydrate_rejectsRemainingGreaterThanCost() {
        assertThatThrownBy(() -> Expense.rehydrate(
                "id-1", "w1", "lunch", TEN, Money.of("10.01"), YESTERDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remaining");
    }

    @Test
    void rehydrate_rejectsNullId() {
        assertThatThrownBy(() -> Expense.rehydrate(
                null, "w1", "lunch", TEN, TEN, YESTERDAY))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rehydrate_allowsFuturePurchaseDate() {
        // create() rejects future dates; rehydrate() does not — historical data wins.
        Expense expense = Expense.rehydrate(
                "id-1", "w1", "lunch", TEN, TEN, TODAY.plusDays(30));

        assertThat(expense.getPurchaseDate()).isEqualTo(TODAY.plusDays(30));
    }
}
