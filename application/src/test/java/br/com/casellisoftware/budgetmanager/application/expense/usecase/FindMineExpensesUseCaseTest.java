package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindMineExpensesUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String CREDIT_CARD_ID = "cc-1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-13T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Test
    void execute_months12_queriesFromFirstDayOfOldestIncludedMonth() {
        FindMineExpensesUseCase useCase = new FindMineExpensesUseCase(expenseRepository, installmentRepository, FIXED_CLOCK);
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "Lunch", Money.of("10.00"),
                LocalDate.of(2026, 5, 10), null, false, null, OWNER_ID);

        when(expenseRepository.findByOwnerIdAndPurchaseDateGreaterThanOrEqual(OWNER_ID, LocalDate.of(2025, 6, 1)))
                .thenReturn(List.of(expense));

        List<ExpenseOutput> result = useCase.execute(12, OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("Lunch");
        assertThat(result.getFirst().installment()).isFalse();
        verify(expenseRepository).findByOwnerIdAndPurchaseDateGreaterThanOrEqual(OWNER_ID, LocalDate.of(2025, 6, 1));
    }

    @Test
    void execute_enrichesInstallmentNumberWhenExpenseHasInstallmentId() {
        FindMineExpensesUseCase useCase = new FindMineExpensesUseCase(expenseRepository, installmentRepository, FIXED_CLOCK);
        Expense expense = Expense.create("wallet-1", CREDIT_CARD_ID, "Notebook", Money.of("100.00"),
                LocalDate.of(2026, 5, 10), null, false, "inst-1", OWNER_ID);
        Installment installment = Installment.create("Notebook", Money.of("1200.00"), Money.of("100.00"),
                12, LocalDate.of(2026, 5, 10), CREDIT_CARD_ID, "source-expense", "wallet-1",
                YearMonth.of(2026, 5), null, FIXED_CLOCK, OWNER_ID);

        when(expenseRepository.findByOwnerIdAndPurchaseDateGreaterThanOrEqual(OWNER_ID, LocalDate.of(2025, 6, 1)))
                .thenReturn(List.of(expense));
        when(installmentRepository.findById("inst-1", OWNER_ID)).thenReturn(Optional.of(installment));

        List<ExpenseOutput> result = useCase.execute(12, OWNER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().installment()).isTrue();
        assertThat(result.getFirst().installmentNumber()).isEqualTo(12);
    }
}
