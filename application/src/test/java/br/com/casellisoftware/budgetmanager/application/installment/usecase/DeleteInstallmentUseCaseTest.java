package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAlreadyDeletedException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
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
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteInstallmentUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock private InstallmentRepository installmentRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private ShareRepository shareRepository;
    @Mock private FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary;
    @Mock private FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary;
    @Mock private PatchBulletBoundary patchBulletBoundary;
    @Mock private DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary;

    private DeleteInstallmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteInstallmentUseCase(
                installmentRepository,
                expenseRepository,
                shareRepository,
                findAllPaymentByExpenseIdBoundary,
                findAllBulletsByIdsBoundary,
                patchBulletBoundary,
                deleteAllPaymentByIdBoundary,
                FIXED_CLOCK);
    }

    @Test
    void execute_happyPath_savesDeletedInstallment() {
        Installment installment = sampleInstallment();
        when(installmentRepository.findById(installment.getId(), OWNER)).thenReturn(Optional.of(installment));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.INSTALLMENT, installment.getId(), OWNER)).thenReturn(false);
        when(expenseRepository.findByInstallmentId(installment.getId(), OWNER)).thenReturn(Optional.empty());

        useCase.execute(installment.getId(), OWNER);

        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        verify(installmentRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isEqualTo(FIXED_CLOCK.instant().atOffset(ZoneOffset.UTC).toLocalDateTime());
    }

    @Test
    void execute_missing_throws() {
        when(installmentRepository.findById("missing", OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing", OWNER))
                .isInstanceOf(InstallmentNotFoundException.class);
        verify(installmentRepository, never()).save(any());
    }

    @Test
    void execute_linkedExpenseWithNoPayments_deletesExpenseWithoutBulletRefund() {
        Installment installment = sampleInstallment();
        Expense linkedExpense = sampleExpense(installment.getId());
        when(installmentRepository.findById(installment.getId(), OWNER)).thenReturn(Optional.of(installment));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.INSTALLMENT, installment.getId(), OWNER)).thenReturn(false);
        when(expenseRepository.findByInstallmentId(installment.getId(), OWNER)).thenReturn(Optional.of(linkedExpense));
        when(findAllPaymentByExpenseIdBoundary.execute(linkedExpense.getId())).thenReturn(List.of());

        useCase.execute(installment.getId(), OWNER);

        verify(expenseRepository).deleteById(linkedExpense.getId(), OWNER);
        verify(patchBulletBoundary, never()).execute(any());
        verify(deleteAllPaymentByIdBoundary, never()).execute(any());
    }

    @Test
    void execute_linkedExpenseWithPayment_refundsBulletAndDeletesPayment() {
        Installment installment = sampleInstallment();
        Expense linkedExpense = sampleExpense(installment.getId());

        PaymentOutput payment = new PaymentOutput(
                "pay-1",
                Money.of(new BigDecimal("200.00")),
                Instant.now(),
                null,
                linkedExpense.getId(),
                "w1",
                "bullet-1"
        );
        BulletOutput bullet = new BulletOutput("bullet-1", "Gastos", new BigDecimal("1000.00"), new BigDecimal("800.00"), "w1");

        when(installmentRepository.findById(installment.getId(), OWNER)).thenReturn(Optional.of(installment));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.INSTALLMENT, installment.getId(), OWNER)).thenReturn(false);
        when(expenseRepository.findByInstallmentId(installment.getId(), OWNER)).thenReturn(Optional.of(linkedExpense));
        when(findAllPaymentByExpenseIdBoundary.execute(linkedExpense.getId())).thenReturn(List.of(payment));
        when(findAllBulletsByIdsBoundary.execute(List.of("bullet-1"))).thenReturn(List.of(bullet));

        useCase.execute(installment.getId(), OWNER);

        // bullet must be credited with the payment amount and have the correct ownerId
        ArgumentCaptor<PatchBulletInput> bulletCaptor = ArgumentCaptor.forClass(PatchBulletInput.class);
        verify(patchBulletBoundary).execute(bulletCaptor.capture());
        assertThat(bulletCaptor.getValue().remaining()).isEqualByComparingTo(new BigDecimal("1000.00")); // 800 + 200
        assertThat(bulletCaptor.getValue().ownerId()).isEqualTo(OWNER);

        // payment must be deleted
        verify(deleteAllPaymentByIdBoundary).execute(List.of("pay-1"));

        // expense must be deleted after refund
        verify(expenseRepository).deleteById(linkedExpense.getId(), OWNER);
    }

    @Test
    void execute_linkedExpenseWithMultiplePaymentsSameBullet_aggregatesRefund() {
        Installment installment = sampleInstallment();
        Expense linkedExpense = sampleExpense(installment.getId());

        PaymentOutput p1 = new PaymentOutput("pay-1", Money.of(new BigDecimal("150.00")), Instant.now(), null, linkedExpense.getId(), "w1", "bullet-1");
        PaymentOutput p2 = new PaymentOutput("pay-2", Money.of(new BigDecimal("50.00")), Instant.now(), null, linkedExpense.getId(), "w1", "bullet-1");
        BulletOutput bullet = new BulletOutput("bullet-1", "Gastos", new BigDecimal("1000.00"), new BigDecimal("800.00"), "w1");

        when(installmentRepository.findById(installment.getId(), OWNER)).thenReturn(Optional.of(installment));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.INSTALLMENT, installment.getId(), OWNER)).thenReturn(false);
        when(expenseRepository.findByInstallmentId(installment.getId(), OWNER)).thenReturn(Optional.of(linkedExpense));
        when(findAllPaymentByExpenseIdBoundary.execute(linkedExpense.getId())).thenReturn(List.of(p1, p2));
        when(findAllBulletsByIdsBoundary.execute(List.of("bullet-1"))).thenReturn(List.of(bullet));

        useCase.execute(installment.getId(), OWNER);

        ArgumentCaptor<PatchBulletInput> bulletCaptor = ArgumentCaptor.forClass(PatchBulletInput.class);
        verify(patchBulletBoundary).execute(bulletCaptor.capture());
        assertThat(bulletCaptor.getValue().remaining()).isEqualByComparingTo(new BigDecimal("1000.00")); // 800 + 150 + 50

        verify(deleteAllPaymentByIdBoundary).execute(List.of("pay-1", "pay-2"));
    }

    @Test
    void execute_alreadyDeleted_propagatesConflictState() {
        Installment deleted = sampleInstallment().delete(FIXED_CLOCK);
        when(installmentRepository.findById(deleted.getId(), OWNER)).thenReturn(Optional.of(deleted));
        when(shareRepository.existsActiveBySourceId(ShareSourceType.INSTALLMENT, deleted.getId(), OWNER)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(deleted.getId(), OWNER))
                .isInstanceOf(InstallmentAlreadyDeletedException.class)
                .hasMessageContaining(deleted.getId());

        verify(installmentRepository, never()).save(any());
    }

    private static Installment sampleInstallment() {
        return Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        );
    }

    private static Expense sampleExpense(String installmentId) {
        return new Expense(
                "exp-1",
                "w1",
                "cc1",
                "Notebook",
                Money.of(new BigDecimal("1000.00")),
                Money.of(new BigDecimal("1000.00")),
                LocalDate.of(2026, 5, 10),
                List.of(),
                FlagEnum.NONE,
                false,
                installmentId
        );
    }
}
