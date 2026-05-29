package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardExpensesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesInput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardFilter;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardResult;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class FindCreditCardExpensesUseCaseTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private FindCreditCardExpensesUseCase useCase;

    @Test
    void execute_withoutMonthFilter_returnsPagedExpensesAndTotalCost() {
        when(creditCardRepository.findById(eq("cc-1"), anyString()))
                .thenReturn(Optional.of(new CreditCard("cc-1", "Nubank")));

        Expense expense = Expense.create("wallet-1", "cc-1", "Uber", Money.of("42.50"), LocalDate.of(2026, 5, 10), null);
        when(expenseRepository.findByCreditCardId(
                "cc-1",
                new ExpenseByCreditCardFilter(null, "Uber"),
                0,
                20,
                "owner-1"
        )).thenReturn(new ExpenseByCreditCardResult(
                new PageResult<>(List.of(expense), 0, 20, 1, 1),
                new BigDecimal("42.50")
        ));

        CreditCardExpensesOutput result = useCase.execute(
                "cc-1",
                new FindCreditCardExpensesInput(null, " Uber ", 0, 20),
                "owner-1"
        );

        assertThat(result.totalCost()).isEqualByComparingTo("42.50");
        assertThat(result.expenses().content()).hasSize(1);
        assertThat(result.expenses().content().getFirst().name()).isEqualTo("Uber");
        assertThat(result.expenses().content().getFirst().creditCardId()).isEqualTo("cc-1");
        verify(walletRepository, never()).findIdsByEffectiveMonth(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void execute_withEffectiveMonthFilter_usesWalletIdsInRepositoryQuery() {
        when(creditCardRepository.findById(eq("cc-1"), anyString()))
                .thenReturn(Optional.of(new CreditCard("cc-1", "Nubank")));
        when(walletRepository.findIdsByEffectiveMonth(YearMonth.of(2026, 5), "owner-1"))
                .thenReturn(List.of("wallet-1", "wallet-2"));
        when(expenseRepository.findByCreditCardId(
                org.mockito.ArgumentMatchers.eq("cc-1"),
                org.mockito.ArgumentMatchers.any(ExpenseByCreditCardFilter.class),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.eq("owner-1")
        )).thenReturn(new ExpenseByCreditCardResult(
                new PageResult<>(List.of(), 1, 10, 0, 0),
                BigDecimal.ZERO
        ));

        useCase.execute("cc-1", new FindCreditCardExpensesInput(
                YearMonth.of(2026, 5),
                null,
                1,
                10
        ), "owner-1");

        ArgumentCaptor<ExpenseByCreditCardFilter> filterCaptor = ArgumentCaptor.forClass(ExpenseByCreditCardFilter.class);
        verify(expenseRepository).findByCreditCardId(
                org.mockito.ArgumentMatchers.eq("cc-1"),
                filterCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq(10),
                org.mockito.ArgumentMatchers.eq("owner-1")
        );

        assertThat(filterCaptor.getValue().walletIds()).containsExactly("wallet-1", "wallet-2");
        assertThat(filterCaptor.getValue().name()).isNull();
    }

    @Test
    void execute_withEffectiveMonthFilterAndNoWallets_returnsEmptyWithoutQueryingExpenses() {
        when(creditCardRepository.findById(eq("cc-1"), anyString()))
                .thenReturn(Optional.of(new CreditCard("cc-1", "Nubank")));
        when(walletRepository.findIdsByEffectiveMonth(YearMonth.of(2026, 7), "owner-1"))
                .thenReturn(List.of());

        CreditCardExpensesOutput result = useCase.execute(
                "cc-1", new FindCreditCardExpensesInput(YearMonth.of(2026, 7), null, 0, 20), "owner-1"
        );

        assertThat(result.totalCost()).isEqualByComparingTo("0");
        assertThat(result.expenses().content()).isEmpty();
        assertThat(result.expenses().totalElements()).isZero();
        verify(expenseRepository, never()).findByCreditCardId(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()
        );
    }

    @Test
    void execute_missingCreditCard_throwsNotFound() {
        when(creditCardRepository.findById(eq("missing"), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(
                "missing",
                new FindCreditCardExpensesInput(null, null, 0, 20),
                "owner-1"
        )).isInstanceOf(CreditCardNotFoundException.class);
    }
}
