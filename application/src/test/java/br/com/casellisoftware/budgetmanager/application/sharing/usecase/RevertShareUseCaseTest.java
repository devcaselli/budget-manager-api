package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevertShareUseCaseTest {

    @Mock
    private ShareRepository shareRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private InstallmentRepository installmentRepository;

    private RevertShareUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RevertShareUseCase(
                shareRepository,
                paymentRepository,
                expenseRepository,
                installmentRepository,
                Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void execute_marksShareRevertedAndCreatesReversalPaymentsAndCreditsExpenseRemaining() {
        Share share = new Share(
                "share-1",
                "owner-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.of("40.00"),
                new BigDecimal("0.40000000"),
                List.of(new br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota("payer-1", new BigDecimal("0.60000000"), List.of("payment-2"))),
                ShareStatus.ACTIVE,
                List.of("payment-1", "payment-2"),
                Instant.parse("2026-05-14T11:00:00Z"),
                null
        );
        Payment ownerPayment = Payment.rebuild(
                "payment-1",
                Money.of("40.00"),
                Instant.parse("2026-05-14T11:30:00Z"),
                "shared owner quota",
                "expense-1",
                "wallet-1",
                null,
                FlagEnum.NONE,
                "owner-1"
        );
        Payment payerPayment = Payment.rebuild(
                "payment-2",
                Money.of("60.00"),
                Instant.parse("2026-05-14T11:30:00Z"),
                "shared payer quota",
                "expense-1",
                "wallet-1",
                null,
                FlagEnum.NONE,
                "owner-1"
        );
        Expense fullyDebited = Expense.create("wallet-1", "cc-1", "Notebook", Money.of("100.00"),
                LocalDate.of(2026, 5, 10), FlagEnum.NONE)
                .debit(Money.of("100.00"));

        when(shareRepository.findById("share-1", "owner-1")).thenReturn(Optional.of(share));
        when(expenseRepository.findById("expense-1", "owner-1")).thenReturn(Optional.of(fullyDebited));
        when(paymentRepository.findById("payment-1", "owner-1")).thenReturn(Optional.of(ownerPayment));
        when(paymentRepository.findById("payment-2", "owner-1")).thenReturn(Optional.of(payerPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("share-1", "owner-1");

        verify(paymentRepository, atLeastOnce()).save(any(Payment.class));
        verify(shareRepository).save(any(Share.class));

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().getRemaining()).isEqualTo(Money.of("100.00"));
        assertThat(captor.getValue().isHidden()).isFalse();
    }

    @Test
    void execute_revertsFullAssignmentExpenseByUnhidingIt() {
        Share share = new Share(
                "share-1",
                "owner-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.zero(),
                BigDecimal.ZERO.setScale(8),
                List.of(new br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota("payer-1", new BigDecimal("1.00000000"), List.of("payment-1"))),
                ShareStatus.ACTIVE,
                List.of("payment-1"),
                Instant.parse("2026-05-14T11:00:00Z"),
                null
        );
        Expense hidden = Expense.create("wallet-1", "cc-1", "Notebook", Money.of("100.00"),
                LocalDate.of(2026, 5, 10), FlagEnum.NONE, true, null, "owner-1");

        when(shareRepository.findById("share-1", "owner-1")).thenReturn(Optional.of(share));
        when(expenseRepository.findById("expense-1", "owner-1")).thenReturn(Optional.of(hidden));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("share-1", "owner-1");

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().isHidden()).isFalse();
    }

    @Test
    void execute_installmentShareRestoresMonthlyExpenseWithoutDeletingIt() {
        Share share = new Share(
                "share-1",
                "owner-1",
                "wallet-1",
                ShareSourceType.INSTALLMENT,
                "installment-1",
                Money.of("1000.00"),
                Money.of("250.00"),
                new BigDecimal("0.25000000"),
                List.of(new br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota("payer-1", new BigDecimal("0.75000000"), List.of())),
                ShareStatus.ACTIVE,
                List.of(),
                Instant.parse("2026-05-14T11:00:00Z"),
                null
        );
        br.com.casellisoftware.budgetmanager.domain.installment.Installment installment =
                br.com.casellisoftware.budgetmanager.domain.installment.Installment.rebuild(
                "installment-1",
                "Phone",
                null,
                Money.of("1000.00"),
                Money.of("100.00"),
                10,
                java.time.LocalDate.of(2026, 5, 10),
                java.time.YearMonth.of(2027, 2),
                "card-1",
                "source-expense-1",
                "wallet-1",
                java.time.YearMonth.of(2026, 5),
                false,
                null,
                FlagEnum.NONE,
                "owner-1"
        );
        Expense reducedMonthlyExpense = new Expense(
                "monthly-expense-1",
                "wallet-1",
                "card-1",
                "Phone",
                Money.of("25.00"),
                Money.of("25.00"),
                java.time.LocalDate.of(2026, 5, 10),
                List.of(),
                FlagEnum.NONE,
                false,
                "installment-1",
                "owner-1"
        );

        when(shareRepository.findById("share-1", "owner-1")).thenReturn(Optional.of(share));
        when(installmentRepository.findById("installment-1", "owner-1")).thenReturn(Optional.of(installment));
        when(expenseRepository.findByInstallmentId("installment-1", "owner-1")).thenReturn(Optional.of(reducedMonthlyExpense));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute("share-1", "owner-1");

        verify(expenseRepository, never()).deleteById("monthly-expense-1", "owner-1");
        ArgumentCaptor<Expense> captor2 = ArgumentCaptor.forClass(Expense.class);
        verify(expenseRepository).save(captor2.capture());
        assertThat(captor2.getValue().getId()).isEqualTo("monthly-expense-1");
        assertThat(captor2.getValue().getCost()).isEqualTo(Money.of("100.00"));
        assertThat(captor2.getValue().getRemaining()).isEqualTo(Money.of("100.00"));
    }

    @Test
    void execute_whenOnePaymentIdNotFound_skipsItAndContinues() {
        // share references two payments but "payment-missing" does not exist in the repo
        Share share = new Share(
                "share-1",
                "owner-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.of("40.00"),
                new BigDecimal("0.40000000"),
                List.of(new br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota(
                        "payer-1", new BigDecimal("0.60000000"), List.of("payment-missing"))),
                ShareStatus.ACTIVE,
                List.of("payment-missing", "payment-ok"),
                Instant.parse("2026-05-14T11:00:00Z"),
                null
        );
        Payment validPayment = Payment.rebuild(
                "payment-ok",
                Money.of("40.00"),
                Instant.parse("2026-05-14T11:30:00Z"),
                "shared owner quota",
                "expense-1",
                "wallet-1",
                null,
                FlagEnum.NONE,
                "owner-1"
        );
        Expense expense = Expense.create("wallet-1", "cc-1", "Notebook", Money.of("100.00"),
                java.time.LocalDate.of(2026, 5, 10), FlagEnum.NONE)
                .debit(Money.of("40.00"));

        when(shareRepository.findById("share-1", "owner-1")).thenReturn(Optional.of(share));
        when(expenseRepository.findById("expense-1", "owner-1")).thenReturn(Optional.of(expense));
        when(paymentRepository.findById("payment-missing", "owner-1")).thenReturn(Optional.empty());
        when(paymentRepository.findById("payment-ok", "owner-1")).thenReturn(Optional.of(validPayment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));

        // must not throw even though one payment id is absent
        useCase.execute("share-1", "owner-1");

        // only one reversal created (for payment-ok), missing one silently skipped
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getAllValues()).hasSize(1);
        assertThat(paymentCaptor.getValue().isReversal()).isTrue();
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(Money.of("40.00"));
    }
}
