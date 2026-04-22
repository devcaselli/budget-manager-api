package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.PatchExpenseInput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchExpenseUseCaseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private PatchExpenseUseCase useCase;

    @Test
    void execute_patchesOnlyFieldsPresentInContract() {
        Expense existing = new Expense(
                "expense-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                LocalDate.of(2026, 4, 21),
                List.of("payment-1")
        );
        PatchExpenseInput input = new PatchExpenseInput(
                "expense-1",
                "dinner",
                new BigDecimal("30.00"),
                LocalDate.of(2026, 4, 22)
        );

        when(expenseRepository.findById("expense-1")).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseOutput output = useCase.execute(input);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo("expense-1");
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        assertThat(saved.getName()).isEqualTo("dinner");
        assertThat(saved.getCost()).isEqualTo(Money.of("30.00"));
        assertThat(saved.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 4, 22));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(saved.getPaymentIds()).containsExactly("payment-1");

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.name()).isEqualTo(saved.getName());
        assertThat(output.cost()).isEqualByComparingTo("30.00");
        assertThat(output.purchaseDate()).isEqualTo(saved.getPurchaseDate());
        assertThat(output.remaining()).isEqualByComparingTo("10.00");
        assertThat(output.paymentIds()).containsExactly("payment-1");
    }

    @Test
    void execute_whenOnlyNameProvided_preservesCostPurchaseDateAndFinancialFields() {
        Expense existing = new Expense(
                "expense-1",
                "wallet-1",
                "lunch",
                Money.of("25.00"),
                Money.of("10.00"),
                LocalDate.of(2026, 4, 21),
                List.of("payment-1")
        );
        PatchExpenseInput input = new PatchExpenseInput(
                "expense-1",
                "dinner",
                null,
                null
        );

        when(expenseRepository.findById("expense-1")).thenReturn(Optional.of(existing));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        Expense saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("dinner");
        assertThat(saved.getCost()).isEqualTo(Money.of("25.00"));
        assertThat(saved.getPurchaseDate()).isEqualTo(LocalDate.of(2026, 4, 21));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("10.00"));
        assertThat(saved.getPaymentIds()).containsExactly("payment-1");
    }

    @Test
    void execute_whenExpenseDoesNotExist_throwsAndDoesNotSave() {
        PatchExpenseInput input = new PatchExpenseInput(
                "expense-missing",
                "dinner",
                null,
                null
        );
        when(expenseRepository.findById("expense-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("expense-missing");

        verify(expenseRepository, never()).save(any());
    }
}
