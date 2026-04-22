package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseInput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.WalletMismatchException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayExpenseUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private BulletRepository bulletRepository;

    @InjectMocks
    private PayExpenseUseCase useCase;

    @Test
    void execute_whenPolicyAccepts_savesPaymentExpenseAndBulletInOrder() {
        Instant paymentDate = Instant.parse("2026-04-22T10:00:00Z");
        Expense expense = new Expense("expense-1", "wallet-1", "Lunch",
                Money.of("100.00"), Money.of("100.00"), LocalDate.of(2026, 4, 22));
        Bullet bullet = Bullet.rebuild("bullet-1", "Food",
                Money.of("80.00"), Money.of("80.00"), "wallet-1");
        Payment savedPayment = Payment.rebuild(
                "payment-1",
                Money.of("30.00"),
                paymentDate,
                "partial payment",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );
        PayExpenseInput input = new PayExpenseInput(
                Money.of("30.00"),
                paymentDate,
                "partial payment",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        when(expenseRepository.findById("expense-1")).thenReturn(Optional.of(expense));
        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(bullet));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        var output = useCase.execute(input);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
        ArgumentCaptor<Bullet> bulletCaptor = ArgumentCaptor.forClass(Bullet.class);
        InOrder inOrder = inOrder(paymentRepository, expenseRepository, bulletRepository);
        inOrder.verify(paymentRepository).save(paymentCaptor.capture());
        inOrder.verify(expenseRepository).save(expenseCaptor.capture());
        inOrder.verify(bulletRepository).save(bulletCaptor.capture());
        inOrder.verifyNoMoreInteractions();

        Payment paymentToSave = paymentCaptor.getValue();
        assertThat(paymentToSave.getAmount()).isEqualTo(input.amount());
        assertThat(paymentToSave.getPaymentDate()).isEqualTo(input.paymentDate());
        assertThat(paymentToSave.getDetails()).isEqualTo(input.details());
        assertThat(paymentToSave.getExpenseId()).isEqualTo(input.expenseId());
        assertThat(paymentToSave.getWalletId()).isEqualTo(input.walletId());
        assertThat(paymentToSave.getBulletId()).isEqualTo(input.bulletId());

        Expense paidExpense = expenseCaptor.getValue();
        assertThat(paidExpense.getRemaining()).isEqualTo(Money.of("70.00"));
        assertThat(paidExpense.getPaymentIds()).containsExactly("payment-1");

        Bullet paidBullet = bulletCaptor.getValue();
        assertThat(paidBullet.getRemaining()).isEqualTo(Money.of("50.00"));

        assertThat(output.id()).isEqualTo(savedPayment.getId());
        assertThat(output.amount()).isEqualTo(savedPayment.getAmount());
        assertThat(output.paymentDate()).isEqualTo(savedPayment.getPaymentDate());
        assertThat(output.details()).isEqualTo(savedPayment.getDetails());
        assertThat(output.expenseId()).isEqualTo(savedPayment.getExpenseId());
        assertThat(output.walletId()).isEqualTo(savedPayment.getWalletId());
        assertThat(output.bulletId()).isEqualTo(savedPayment.getBulletId());
    }

    @Test
    void execute_validatesPolicyBeforeAnySave() {
        Expense expense = new Expense("expense-1", "wallet-1", "Lunch",
                Money.of("100.00"), Money.of("100.00"), LocalDate.now());
        Bullet bullet = Bullet.rebuild("bullet-1", "Food",
                Money.of("100.00"), Money.of("100.00"), "wallet-2");
        PayExpenseInput input = new PayExpenseInput(
                Money.of("50.00"),
                Instant.parse("2026-04-22T10:00:00Z"),
                "partial payment",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        when(expenseRepository.findById("expense-1")).thenReturn(Optional.of(expense));
        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(bullet));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletMismatchException.class);

        verify(paymentRepository, never()).save(any(Payment.class));
        verify(expenseRepository, never()).save(any(Expense.class));
        verify(bulletRepository, never()).save(any(Bullet.class));
    }
}
