package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.usecase.EnsureTransientPayerUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareQuotaInput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareAlreadyActiveForSourceException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveShareUseCaseTest {

    @Mock
    private ShareRepository shareRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PayerRepository payerRepository;
    @Mock
    private EnsureTransientPayerUseCase ensureTransientPayerUseCase;

    private SaveShareUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-14T12:00:00Z"), ZoneOffset.UTC);
        useCase = new SaveShareUseCase(
                shareRepository,
                expenseRepository,
                subscriptionRepository,
                installmentRepository,
                paymentRepository,
                payerRepository,
                ensureTransientPayerUseCase,
                clock
        );
    }

    @Test
    void execute_expensePartialShare_debitsOnlyPayerQuotaAndLeavesOwnerPortionForBulletFlow() {
        Expense expense = new Expense(
                "expense-1",
                "wallet-1",
                "card-1",
                "Notebook",
                Money.of("100.00"),
                Money.of("100.00"),
                LocalDate.of(2026, 5, 10),
                List.of(),
                FlagEnum.NONE,
                false,
                null,
                "owner-1"
        );

        when(shareRepository.existsActiveBySourceId(ShareSourceType.EXPENSE, "expense-1", "owner-1")).thenReturn(false);
        when(expenseRepository.findById("expense-1", "owner-1")).thenReturn(Optional.of(expense));
        org.mockito.Mockito.lenient().when(payerRepository.existsById("payer-1", "owner-1")).thenReturn(true);
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payerRepository.findAllByIdsIn(any(), any())).thenReturn(List.of(
                new Payer("payer-1", "owner-1", "Maria", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), false)
        ));
        org.mockito.Mockito.lenient().when(payerRepository.findById("payer-1", "owner-1")).thenReturn(Optional.of(
                new Payer("payer-1", "owner-1", "Maria", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), false)
        ));

        ShareOutput output = useCase.execute(new ShareInput(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new java.math.BigDecimal("100.00"),
                "BRL",
                new java.math.BigDecimal("40.00"),
                List.of(new ShareQuotaInput("payer-1", null, new java.math.BigDecimal("60.00"))),
                "owner-1"
        ));

        assertThat(output.sourceType()).isEqualTo(ShareSourceType.EXPENSE);
        assertThat(output.ownerRatio()).isEqualByComparingTo("0.40000000");
        assertThat(output.paymentIds()).hasSize(1);

        org.mockito.ArgumentCaptor<Expense> expenseCaptor = org.mockito.ArgumentCaptor.forClass(Expense.class);
        org.mockito.Mockito.verify(expenseRepository).save(expenseCaptor.capture());
        assertThat(expenseCaptor.getValue().getRemaining()).isEqualTo(Money.of("40.00"));
        assertThat(expenseCaptor.getValue().getCost()).isEqualTo(Money.of("100.00"));
        assertThat(expenseCaptor.getValue().isHidden()).isFalse();

        org.mockito.ArgumentCaptor<Payment> paymentCaptor = org.mockito.ArgumentCaptor.forClass(Payment.class);
        org.mockito.Mockito.verify(paymentRepository).save(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getPayerId()).isEqualTo("payer-1");
        assertThat(paymentCaptor.getValue().getAmount()).isEqualTo(Money.of("60.00"));
    }

    @Test
    void execute_expenseFullAssignment_hidesSourceExpense() {
        Expense expense = new Expense(
                "expense-1",
                "wallet-1",
                "card-1",
                "Notebook",
                Money.of("100.00"),
                Money.of("100.00"),
                LocalDate.of(2026, 5, 10),
                List.of(),
                FlagEnum.NONE,
                false,
                null,
                "owner-1"
        );

        when(shareRepository.existsActiveBySourceId(ShareSourceType.EXPENSE, "expense-1", "owner-1")).thenReturn(false);
        when(expenseRepository.findById("expense-1", "owner-1")).thenReturn(Optional.of(expense));
        org.mockito.Mockito.lenient().when(payerRepository.existsById("payer-1", "owner-1")).thenReturn(true);
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(payerRepository.findAllByIdsIn(any(), any())).thenReturn(List.of(
                new Payer("payer-1", "owner-1", "Maria", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), false)
        ));
        org.mockito.Mockito.lenient().when(payerRepository.findById("payer-1", "owner-1")).thenReturn(Optional.of(
                new Payer("payer-1", "owner-1", "Maria", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), false)
        ));

        useCase.execute(new ShareInput(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new java.math.BigDecimal("100.00"),
                "BRL",
                java.math.BigDecimal.ZERO,
                List.of(new ShareQuotaInput("payer-1", null, new java.math.BigDecimal("100.00"))),
                "owner-1"
        ));

        org.mockito.ArgumentCaptor<Expense> captor = org.mockito.ArgumentCaptor.forClass(Expense.class);
        org.mockito.Mockito.verify(expenseRepository).save(captor.capture());
        assertThat(captor.getValue().isHidden()).isTrue();
    }

    @Test
    void execute_whenShareAlreadyActive_throwsConflict() {
        when(shareRepository.existsActiveBySourceId(ShareSourceType.EXPENSE, "expense-1", "owner-1")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new ShareInput(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new java.math.BigDecimal("100.00"),
                "BRL",
                new java.math.BigDecimal("40.00"),
                List.of(new ShareQuotaInput("payer-1", null, new java.math.BigDecimal("60.00"))),
                "owner-1"
        ))).isInstanceOf(ShareAlreadyActiveForSourceException.class);
    }

    @Test
    void execute_whenExpenseSaveFails_propagatesExceptionAndDoesNotPersistPayments() {
        Expense expense = new Expense(
                "expense-1",
                "wallet-1",
                "card-1",
                "Notebook",
                Money.of("100.00"),
                Money.of("100.00"),
                LocalDate.of(2026, 5, 10),
                List.of(),
                br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE,
                false,
                null,
                "owner-1"
        );

        when(shareRepository.existsActiveBySourceId(ShareSourceType.EXPENSE, "expense-1", "owner-1")).thenReturn(false);
        when(expenseRepository.findById("expense-1", "owner-1")).thenReturn(Optional.of(expense));
        when(payerRepository.findById("payer-1", "owner-1")).thenReturn(Optional.of(
                new Payer("payer-1", "owner-1", "Maria",
                        PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), false)
        ));
        when(shareRepository.save(any(Share.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("DB unavailable")).when(expenseRepository).save(any(Expense.class));

        assertThatThrownBy(() -> useCase.execute(new ShareInput(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new java.math.BigDecimal("100.00"),
                "BRL",
                new java.math.BigDecimal("40.00"),
                List.of(new ShareQuotaInput("payer-1", null, new java.math.BigDecimal("60.00"))),
                "owner-1"
        ))).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB unavailable");

        // appendPayments save must not be reached after mid-cascade failure
        verify(shareRepository, never()).save(
                org.mockito.ArgumentMatchers.argThat(s -> s.getPaymentIds() != null && !s.getPaymentIds().isEmpty()));
    }
}
