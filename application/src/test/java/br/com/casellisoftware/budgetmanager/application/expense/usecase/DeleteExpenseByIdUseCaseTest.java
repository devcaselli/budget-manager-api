package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteExpenseByIdUseCaseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary;

    @Mock
    private FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary;

    @Mock
    private PatchBulletBoundary patchBulletBoundary;

    @Mock
    private DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary;

    @InjectMocks
    private DeleteExpenseByIdUseCase useCase;

    @Test
    void execute_whenExpenseDoesNotExist_throwsAndDoesNotCascade() {
        when(expenseRepository.existsById("expense-missing")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("expense-missing"))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessageContaining("expense-missing");

        verify(expenseRepository, never()).deleteById("expense-missing");
        verifyNoInteractions(
                findAllPaymentByExpenseIdBoundary,
                findAllBulletsByIdsBoundary,
                patchBulletBoundary,
                deleteAllPaymentByIdBoundary
        );
    }

    @Test
    void execute_whenExpenseHasNoPayments_deletesExpenseOnly() {
        when(expenseRepository.existsById("expense-1")).thenReturn(true);
        when(findAllPaymentByExpenseIdBoundary.execute("expense-1")).thenReturn(List.of());

        useCase.execute("expense-1");

        verify(expenseRepository).existsById("expense-1");
        verify(findAllPaymentByExpenseIdBoundary).execute("expense-1");
        verify(expenseRepository).deleteById("expense-1");
        verifyNoInteractions(
                findAllBulletsByIdsBoundary,
                patchBulletBoundary,
                deleteAllPaymentByIdBoundary
        );
    }

    @Test
    void execute_whenExpenseHasPayments_rechargesBulletsDeletesPaymentsAndExpense() {
        when(expenseRepository.existsById("expense-1")).thenReturn(true);
        when(findAllPaymentByExpenseIdBoundary.execute("expense-1")).thenReturn(List.of(
                payment("payment-1", "bullet-1", "10.00"),
                payment("payment-2", "bullet-1", "15.00"),
                payment("payment-3", "bullet-2", "5.00")
        ));
        when(findAllBulletsByIdsBoundary.execute(anyList())).thenReturn(List.of(
                new BulletOutput("bullet-1", "food", new BigDecimal("100.00"), new BigDecimal("40.00"), "wallet-1"),
                new BulletOutput("bullet-2", "transport", new BigDecimal("80.00"), new BigDecimal("20.00"), "wallet-1")
        ));

        useCase.execute("expense-1");

        ArgumentCaptor<PatchBulletInput> patchCaptor = ArgumentCaptor.forClass(PatchBulletInput.class);
        verify(patchBulletBoundary, org.mockito.Mockito.times(2)).execute(patchCaptor.capture());

        assertThat(patchCaptor.getAllValues())
                .extracting(PatchBulletInput::id, PatchBulletInput::remaining)
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("bullet-1", new BigDecimal("65.00")),
                        org.assertj.core.api.Assertions.tuple("bullet-2", new BigDecimal("25.00"))
                );

        verify(deleteAllPaymentByIdBoundary).execute(List.of("payment-1", "payment-2", "payment-3"));
        verify(expenseRepository).deleteById("expense-1");
    }

    private static PaymentOutput payment(String id, String bulletId, String amount) {
        return new PaymentOutput(
                id,
                Money.of(amount),
                Instant.parse("2026-04-21T10:00:00Z"),
                "details",
                "expense-1",
                "wallet-1",
                bulletId
        );
    }
}
