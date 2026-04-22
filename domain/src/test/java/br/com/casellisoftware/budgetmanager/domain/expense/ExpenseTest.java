package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

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

    @Test
    void create_rejectsNullOrBlankName() {
        assertThatThrownBy(() -> Expense.create("w1", null, TEN, YESTERDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Expense.create("w1", "   ", TEN, YESTERDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsNameExceedingMaxLength() {
        String tooLong = "x".repeat(Expense.MAX_NAME_LENGTH + 1);
        assertThatThrownBy(() -> Expense.create("w1", tooLong, TEN, YESTERDAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(Expense.MAX_NAME_LENGTH));
    }

    @Test
    void create_acceptsNameAtMaxLength() {
        String atLimit = "x".repeat(Expense.MAX_NAME_LENGTH);
        Expense expense = Expense.create("w1", atLimit, TEN, YESTERDAY);
        assertThat(expense.getName()).isEqualTo(atLimit);
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

    // ---- patch ----

    @Test
    void patch_onlyUpdatesNonNullFields() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1")
        );

        ExpensePatch patch = ExpensePatch.empty()
                .withName("dinner")
                .withCost(Money.of("30.00"))
                .withPurchaseDate(null);
        Expense patched = expense.patch(patch);

        assertThat(patched.getId()).isEqualTo("id-1");
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
        assertThat(patched.getName()).isEqualTo("dinner");
        assertThat(patched.getCost()).isEqualTo(Money.of("30.00"));
        assertThat(patched.getPurchaseDate()).isEqualTo(YESTERDAY);
        assertThat(patched.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(patched.getPaymentIds()).containsExactly("payment-1");
    }

    @Test
    void patch_onlyCostUpdatesCostAndPreservesOtherFields() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1")
        );

        Expense patched = expense.patch(ExpensePatch.empty().withCost(Money.of("30.00")));

        assertThat(patched.getName()).isEqualTo("lunch");
        assertThat(patched.getCost()).isEqualTo(Money.of("30.00"));
        assertThat(patched.getPurchaseDate()).isEqualTo(YESTERDAY);
        assertThat(patched.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(patched.getPaymentIds()).containsExactly("payment-1");
    }

    @Test
    void patch_onlyPurchaseDateUpdatesPurchaseDateAndPreservesOtherFields() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1")
        );

        Expense patched = expense.patch(ExpensePatch.empty().withPurchaseDate(TODAY));

        assertThat(patched.getName()).isEqualTo("lunch");
        assertThat(patched.getCost()).isEqualTo(Money.of("25.00"));
        assertThat(patched.getPurchaseDate()).isEqualTo(TODAY);
        assertThat(patched.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(patched.getPaymentIds()).containsExactly("payment-1");
    }

    @Test
    void patch_allNull_returnsSameState() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1")
        );

        Expense patched = expense.patch(ExpensePatch.empty());

        assertThat(patched).isSameAs(expense);
        assertThat(patched.getName()).isEqualTo("lunch");
        assertThat(patched.getCost()).isEqualTo(Money.of("25.00"));
        assertThat(patched.getPurchaseDate()).isEqualTo(YESTERDAY);
        assertThat(patched.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(patched.getPaymentIds()).containsExactly("payment-1");
    }

    @Test
    void patch_sameValues_returnsSameState() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1")
        );

        Expense patched = expense.patch(ExpensePatch.empty()
                .withName("lunch")
                .withCost(Money.of("25.00"))
                .withPurchaseDate(YESTERDAY));

        assertThat(patched).isSameAs(expense);
    }

    @Test
    void patch_rejectsNameExceedingMaxLength() {
        Expense expense = Expense.create("wallet-1", "lunch", TEN, YESTERDAY);
        String tooLong = "x".repeat(Expense.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> expense.patch(ExpensePatch.empty().withName(tooLong)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(Expense.MAX_NAME_LENGTH));
    }

    @Test
    void patch_rejectsNullPatch() {
        Expense expense = Expense.create("wallet-1", "lunch", TEN, YESTERDAY);

        assertThatThrownBy(() -> expense.patch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("patch");
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
