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

    @Test
    void from_copiesAllFields_andFlattensMoneyToAmount() {
        Expense expense = Expense.create("wallet-1", "lunch", Money.of("10.50"), PURCHASE_DATE);

        ExpenseOutput output = ExpenseOutputAssembler.from(expense);

        assertThat(output.id()).isEqualTo(expense.getId());
        assertThat(output.name()).isEqualTo("lunch");
        assertThat(output.cost()).isEqualByComparingTo("10.50");
        assertThat(output.purchaseDate()).isEqualTo(PURCHASE_DATE);
        assertThat(output.walletId()).isEqualTo("wallet-1");
        assertThat(output.remaining()).isEqualByComparingTo("10.50");
    }

    @Test
    void from_afterDebit_reflectsReducedRemaining() {
        Expense original = Expense.create("wallet-1", "coffee", Money.of("20.00"), PURCHASE_DATE);
        Expense debited = original.debit(Money.of("7.50"));

        ExpenseOutput output = ExpenseOutputAssembler.from(debited);

        assertThat(output.cost()).isEqualByComparingTo("20.00");
        assertThat(output.remaining()).isEqualByComparingTo("12.50");
    }
}
