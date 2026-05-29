package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpenseTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);
    private static final Money TEN = Money.of("10.00");
    private static final String CREDIT_CARD_ID = "cc-1";

    @Test
    void create_happyPath_generatesIdAndInitializesRemaining() {
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null);

        assertThat(expense.getId()).isNotBlank();
        assertThat(expense.getWalletId()).isEqualTo("wallet-1");
        assertThat(expense.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(expense.getName()).isEqualTo("lunch");
        assertThat(expense.getCost()).isEqualTo(TEN);
        assertThat(expense.getRemaining()).isEqualTo(TEN);
        assertThat(expense.getPurchaseDate()).isEqualTo(YESTERDAY);
    }

    @Test
    void create_hiddenExpense_marksExpenseAsHidden() {
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null, true);

        assertThat(expense.isHidden()).isTrue();
    }

    @Test
    void create_rejectsZeroCost() {
        assertThatThrownBy(() -> Expense.create("w1", CREDIT_CARD_ID, "lunch", Money.zero(), TODAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cost");
    }

    @Test
    void create_rejectsFuturePurchaseDate() {
        assertThatThrownBy(() -> Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, TODAY.plusDays(1), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void create_rejectsNullOrBlankName() {
        assertThatThrownBy(() -> Expense.create("w1", CREDIT_CARD_ID, null, TEN, YESTERDAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Expense.create("w1", CREDIT_CARD_ID, "   ", TEN, YESTERDAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void create_rejectsNameExceedingMaxLength() {
        String tooLong = "x".repeat(Expense.MAX_NAME_LENGTH + 1);
        assertThatThrownBy(() -> Expense.create("w1", CREDIT_CARD_ID, tooLong, TEN, YESTERDAY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(Expense.MAX_NAME_LENGTH));
    }

    @Test
    void create_acceptsNameAtMaxLength() {
        String atLimit = "x".repeat(Expense.MAX_NAME_LENGTH);
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, atLimit, TEN, YESTERDAY, null);
        assertThat(expense.getName()).isEqualTo(atLimit);
    }

    @Test
    void debit_reducesRemaining() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, TODAY, null);

        Expense after = expense.debit(Money.of("4.00"));

        assertThat(after.getRemaining()).isEqualTo(Money.of("6.00"));
        assertThat(after.getCost()).isEqualTo(TEN);
        assertThat(after.getId()).isEqualTo(expense.getId());
        assertThat(after.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(expense.getRemaining()).isEqualTo(TEN);
    }

    @Test
    void debit_rejectsAmountExceedingRemaining() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, TODAY, null);
        assertThatThrownBy(() -> expense.debit(Money.of("10.01")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds remaining");
    }

    @Test
    void debit_rejectsNonPositiveAmount() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, TODAY, null);
        assertThatThrownBy(() -> expense.debit(Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void expenseEqualsById_evenAfterDebit() {
        Expense original = new Expense("expense-1", "wallet-1", CREDIT_CARD_ID, "lunch", TEN, TEN, TODAY, List.of(), null);
        Payment payment = Payment.rebuild(
                "payment-1",
                Money.of("4.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "partial payment",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        Set<Expense> expenses = new HashSet<>(Set.of(original));
        Expense debited = original.pay(payment);

        assertThat(debited).isEqualTo(original);
        assertThat(expenses).contains(debited);
    }

    @Test
    void pay_debitsRemainingAndAppendsPaymentId() {
        Expense original = new Expense(
                "expense-1",
                "wallet-1",
                CREDIT_CARD_ID,
                "lunch",
                TEN,
                TEN,
                TODAY,
                List.of("payment-0"),
                null
        );
        Payment payment = Payment.rebuild(
                "payment-1",
                Money.of("4.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "partial payment",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        Expense paid = original.pay(payment);

        assertThat(paid.getRemaining()).isEqualTo(Money.of("6.00"));
        assertThat(paid.getPaymentIds()).containsExactly("payment-0", "payment-1");
        assertThat(paid.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(original.getRemaining()).isEqualTo(TEN);
        assertThat(original.getPaymentIds()).containsExactly("payment-0");
    }

    @Test
    void patch_onlyUpdatesNonNullFields() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                CREDIT_CARD_ID,
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1"),
                null
        );

        ExpensePatch patch = ExpensePatch.empty()
                .withName("dinner")
                .withCost(Money.of("30.00"))
                .withPurchaseDate(null);
        Expense patched = expense.patch(patch);

        assertThat(patched.getId()).isEqualTo("id-1");
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
        assertThat(patched.getCreditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(patched.getName()).isEqualTo("dinner");
        assertThat(patched.getCost()).isEqualTo(Money.of("30.00"));
        assertThat(patched.getPurchaseDate()).isEqualTo(YESTERDAY);
        assertThat(patched.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(patched.getPaymentIds()).containsExactly("payment-1");
    }

    @Test
    void patch_allNull_returnsSameState() {
        Expense expense = new Expense(
                "id-1",
                "wallet-1",
                CREDIT_CARD_ID,
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                YESTERDAY,
                List.of("payment-1"),
                null
        );

        Expense patched = expense.patch(ExpensePatch.empty());

        assertThat(patched).isSameAs(expense);
    }

    @Test
    void patch_rejectsNullPatch() {
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null);

        assertThatThrownBy(() -> expense.patch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("patch");
    }

    @Test
    void constructor_allowsPartiallyPaidState() {
        Expense expense = new Expense("id-1", "w1", CREDIT_CARD_ID, "lunch", TEN, Money.of("3.00"), YESTERDAY, null, null);

        assertThat(expense.getRemaining()).isEqualTo(Money.of("3.00"));
        assertThat(expense.getCost()).isEqualTo(TEN);
    }

    @Test
    void constructor_requiresCreditCardId() {
        assertThatThrownBy(() -> new Expense("id-1", "w1", null, "lunch", TEN, TEN, YESTERDAY, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("creditCardId");
    }

    @Test
    void constructor_allowsFuturePurchaseDate() {
        Expense expense = new Expense("id-1", "w1", CREDIT_CARD_ID, "lunch", TEN, TEN, TODAY.plusDays(30), null, null);

        assertThat(expense.getPurchaseDate()).isEqualTo(TODAY.plusDays(30));
    }

    @Test
    void hide_marksExpenseAsHidden() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null);

        Expense hidden = expense.hide();

        assertThat(hidden.isHidden()).isTrue();
        assertThat(expense.isHidden()).isFalse();
    }

    @Test
    void hide_isIdempotent_returnsSameInstanceWhenAlreadyHidden() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null, true);

        Expense hidden = expense.hide();

        assertThat(hidden).isSameAs(expense);
    }

    @Test
    void unhide_marksExpenseAsVisible() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null, true);

        Expense visible = expense.unhide();

        assertThat(visible.isHidden()).isFalse();
        assertThat(expense.isHidden()).isTrue();
    }

    @Test
    void unhide_isIdempotent_returnsSameInstanceWhenAlreadyVisible() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null);

        Expense visible = expense.unhide();

        assertThat(visible).isSameAs(expense);
    }

    @Test
    void credit_increasesRemainingByAmount() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", Money.of("50.00"), YESTERDAY, null)
                .debit(Money.of("20.00"));

        Expense credited = expense.credit(Money.of("5.00"));

        assertThat(credited.getRemaining()).isEqualTo(Money.of("35.00"));
        assertThat(credited.getCost()).isEqualTo(Money.of("50.00"));
    }

    @Test
    void credit_rejectsAmountThatOverflowsCost() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", Money.of("10.00"), YESTERDAY, null)
                .debit(Money.of("3.00"));

        assertThatThrownBy(() -> expense.credit(Money.of("5.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit overflows expense cost");
    }

    @Test
    void debitAndCredit_areSymmetric() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", Money.of("100.00"), YESTERDAY, null);

        Expense roundTripped = expense.debit(Money.of("40.00")).credit(Money.of("40.00"));

        assertThat(roundTripped.getRemaining()).isEqualTo(expense.getRemaining());
    }

    @Test
    void hideAndUnhide_areSymmetric() {
        Expense expense = Expense.create("w1", CREDIT_CARD_ID, "lunch", TEN, YESTERDAY, null);

        Expense roundTripped = expense.hide().unhide();

        assertThat(roundTripped.isHidden()).isFalse();
    }
}
