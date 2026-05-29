package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveExpenseUseCaseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    @Mock
    private FindCreditCardByIdBoundary findCreditCardByIdBoundary;

    private SaveExpenseUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveExpenseUseCase(
                expenseRepository,
                installmentRepository,
                findWalletDomainByIdBoundary,
                findCreditCardByIdBoundary
        );
    }

    @Test
    void execute_happyPath_savesExpenseWithCreditCardId() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(eq("w1"), anyString())).thenReturn(wallet);
        when(findCreditCardByIdBoundary.findById(eq("cc1"), anyString())).thenReturn(new CreditCardOutput("cc1", "Nubank", java.util.List.of()));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseOutput out = useCase.execute(new ExpenseInput(
                "Coffee",
                new BigDecimal("12.34"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                false,
                null,
                FlagEnum.NONE
        ));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getCreditCardId()).isEqualTo("cc1");
        assertThat(out.creditCardId()).isEqualTo("cc1");
    }

    @Test
    void execute_missingCreditCard_propagatesException() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(eq("w1"), anyString())).thenReturn(wallet);
        when(findCreditCardByIdBoundary.findById(eq("missing"), anyString()))
                .thenThrow(new CreditCardNotFoundException("missing"));

        assertThatThrownBy(() -> useCase.execute(new ExpenseInput(
                "Coffee",
                new BigDecimal("12.34"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "missing",
                false,
                null,
                FlagEnum.NONE
        )))
                .isInstanceOf(CreditCardNotFoundException.class);

        verify(expenseRepository, never()).save(any());
        verify(installmentRepository, never()).save(any());
    }

    @Test
    void execute_installmentFalse_savesOnlyExpense() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(eq("w1"), anyString())).thenReturn(wallet);
        when(findCreditCardByIdBoundary.findById(eq("cc1"), anyString()))
                .thenReturn(new CreditCardOutput("cc1", "Nubank", java.util.List.of()));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(new ExpenseInput(
                "Coffee",
                new BigDecimal("12.34"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                false,
                null,
                FlagEnum.NONE
        ));

        verify(installmentRepository, never()).save(any());
    }

    @Test
    void execute_installmentTrue_savesExpenseAndInstallment() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(eq("w1"), anyString())).thenReturn(wallet);
        when(findCreditCardByIdBoundary.findById(eq("cc1"), anyString()))
                .thenReturn(new CreditCardOutput("cc1", "Nubank", java.util.List.of()));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepository.save(any(Installment.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseOutput output = useCase.execute(new ExpenseInput(
                "Notebook",
                new BigDecimal("6000.00"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                true,
                6,
                FlagEnum.NONE
        ));

        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        verify(installmentRepository).save(captor.capture());
        assertThat(captor.getValue().getInstallmentNumber()).isEqualTo(6);
        assertThat(captor.getValue().getInstallmentValue().amount()).isEqualByComparingTo("1000.00");
        assertThat(captor.getValue().getLastInstallmentDate()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(captor.getValue().getSourceWalletId()).isEqualTo("w1");
        assertThat(captor.getValue().getSourceEffectiveMonth()).isEqualTo(YearMonth.of(2026, 5));
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository, org.mockito.Mockito.times(2)).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getAllValues().get(0).isHidden()).isTrue();
        assertThat(expenseCaptor.getAllValues().get(0).getCost().amount()).isEqualByComparingTo("6000.00");
        assertThat(expenseCaptor.getAllValues().get(1).isHidden()).isFalse();
        assertThat(expenseCaptor.getAllValues().get(1).getCost().amount()).isEqualByComparingTo("1000.00");
        assertThat(output.cost()).isEqualByComparingTo("1000.00");
    }

    @Test
    void execute_installmentTrueWithoutInstallmentNumber_failsFast() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(eq("w1"), anyString())).thenReturn(wallet);
        when(findCreditCardByIdBoundary.findById(eq("cc1"), anyString()))
                .thenReturn(new CreditCardOutput("cc1", "Nubank", java.util.List.of()));

        assertThatThrownBy(() -> useCase.execute(new ExpenseInput(
                "Notebook",
                new BigDecimal("6000.00"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                true,
                null,
                FlagEnum.NONE
        )))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("installmentNumber must not be null when installment is true");

        verify(installmentRepository, never()).save(any());
    }

    @Test
    void execute_installmentTrueWithInvalidInstallmentNumber_failsBeforeSavingSourceExpense() {
        Wallet wallet = wallet();
        when(findWalletDomainByIdBoundary.findById(eq("w1"), anyString())).thenReturn(wallet);
        when(findCreditCardByIdBoundary.findById(eq("cc1"), anyString()))
                .thenReturn(new CreditCardOutput("cc1", "Nubank", java.util.List.of()));

        assertThatThrownBy(() -> useCase.execute(new ExpenseInput(
                "Notebook",
                new BigDecimal("6000.00"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                true,
                1,
                FlagEnum.NONE
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("installmentNumber must be >= 2, got 1");

        verify(expenseRepository, never()).save(any());
        verify(installmentRepository, never()).save(any());
    }

    private static Wallet wallet() {
        return new Wallet(
                "w1",
                "May Budget",
                Money.of(new BigDecimal("10000.00")),
                Money.of(new BigDecimal("10000.00")),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                YearMonth.of(2026, 5),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }
}
