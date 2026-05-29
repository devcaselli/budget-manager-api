package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseOutputAssemblerTest {

    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(1);
    private static final String CREDIT_CARD_ID = "cc-1";

    @Test
    void from_copiesAllFields_andFlattensMoneyToAmount() {
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "lunch", Money.of("10.50"), PURCHASE_DATE, null);

        ExpenseOutput output = ExpenseOutputAssembler.from(expense);

        assertThat(output.id()).isEqualTo(expense.getId());
        assertThat(output.name()).isEqualTo("lunch");
        assertThat(output.cost()).isEqualByComparingTo("10.50");
        assertThat(output.purchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(output.walletId()).isEqualTo("wallet-1");
        assertThat(output.creditCardId()).isEqualTo(CREDIT_CARD_ID);
        assertThat(output.remaining()).isEqualByComparingTo("10.50");
        assertThat(output.installment()).isFalse();
        assertThat(output.installmentId()).isNull();
        assertThat(output.installmentNumber()).isNull();
    }

    @Test
    void from_installmentLinkedExpense_exposesInstallmentIdAndNumber() {
        Expense expense = Expense.create(
                "wallet-1",
                CREDIT_CARD_ID,
                "TV",
                Money.of("200.00"),
                PURCHASE_DATE,
                null,
                false,
                "inst-1",
                "owner-1"
        );

        ExpenseOutput output = ExpenseOutputAssembler.from(expense, 3);

        assertThat(output.installment()).isTrue();
        assertThat(output.installmentId()).isEqualTo("inst-1");
        assertThat(output.installmentNumber()).isEqualTo(3);
    }

    @Test
    void from_afterDebit_reflectsReducedRemaining() {
        Expense original = Expense.create("wallet-1", CREDIT_CARD_ID, "coffee", Money.of("20.00"), PURCHASE_DATE, null);
        Expense debited = original.debit(Money.of("7.50"));

        ExpenseOutput output = ExpenseOutputAssembler.from(debited);

        assertThat(output.cost()).isEqualByComparingTo("20.00");
        assertThat(output.remaining()).isEqualByComparingTo("12.50");
    }
}
