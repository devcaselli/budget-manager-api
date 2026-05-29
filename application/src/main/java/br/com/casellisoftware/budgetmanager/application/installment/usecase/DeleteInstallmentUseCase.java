package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.DeleteInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.SourceInUseByShareException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DeleteInstallmentUseCase implements DeleteInstallmentBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteInstallmentUseCase.class);

    private final InstallmentRepository installmentRepository;
    private final ExpenseRepository expenseRepository;
    private final ShareRepository shareRepository;
    private final FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary;
    private final FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary;
    private final PatchBulletBoundary patchBulletBoundary;
    private final DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary;
    private final Clock clock;

    public DeleteInstallmentUseCase(InstallmentRepository installmentRepository,
                                    ExpenseRepository expenseRepository,
                                    ShareRepository shareRepository,
                                    FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary,
                                    FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary,
                                    PatchBulletBoundary patchBulletBoundary,
                                    DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary,
                                    Clock clock) {
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.shareRepository = Objects.requireNonNull(shareRepository, "shareRepository must not be null");
        this.findAllPaymentByExpenseIdBoundary = Objects.requireNonNull(findAllPaymentByExpenseIdBoundary, "findAllPaymentByExpenseIdBoundary must not be null");
        this.findAllBulletsByIdsBoundary = Objects.requireNonNull(findAllBulletsByIdsBoundary, "findAllBulletsByIdsBoundary must not be null");
        this.patchBulletBoundary = Objects.requireNonNull(patchBulletBoundary, "patchBulletBoundary must not be null");
        this.deleteAllPaymentByIdBoundary = Objects.requireNonNull(deleteAllPaymentByIdBoundary, "deleteAllPaymentByIdBoundary must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public void execute(String id, String ownerId) {
        Installment installment = installmentRepository.findById(id, ownerId)
                .orElseThrow(() -> new InstallmentNotFoundException(id));
        if (shareRepository.existsActiveBySourceId(ShareSourceType.INSTALLMENT, id, ownerId)) {
            throw new SourceInUseByShareException(ShareSourceType.INSTALLMENT, id);
        }
        Installment deleted = installment.delete(clock);
        installmentRepository.save(deleted);
        expenseRepository.findByInstallmentId(id, ownerId).ifPresent(expense -> {
            refundBulletsForExpense(expense, ownerId);
            expenseRepository.deleteById(expense.getId(), ownerId);
            log.info("Deleted installment expense id={} installmentId={}", expense.getId(), id);
        });
        log.info("Installment logically deleted id={} at={}", deleted.getId(), deleted.getDeletedAt());
    }

    /**
     * Refunds all bullets charged by payments of the given expense, then deletes those payments.
     * Mirrors the same logic in DeleteExpenseByIdUseCase to ensure bullet balances are restored
     * when an installment's child expense is cascade-deleted.
     *
     * @implNote Time complexity: O(n) where n = number of payments on the expense.
     */
    private void refundBulletsForExpense(Expense expense, String ownerId) {
        List<PaymentOutput> payments = findAllPaymentByExpenseIdBoundary.execute(expense.getId());
        if (payments.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> refundsByBulletId = payments.stream()
                .collect(Collectors.groupingBy(
                        PaymentOutput::bulletId,
                        Collectors.reducing(BigDecimal.ZERO, p -> p.amount().amount(), BigDecimal::add)
                ));

        findAllBulletsByIdsBoundary.execute(List.copyOf(refundsByBulletId.keySet()))
                .forEach(bullet -> patchBulletBoundary.execute(
                        toPatchInput(bullet, refundsByBulletId.get(bullet.id()), ownerId)
                ));

        deleteAllPaymentByIdBoundary.execute(
                payments.stream().map(PaymentOutput::id).toList()
        );
    }

    private PatchBulletInput toPatchInput(BulletOutput bullet, BigDecimal refund, String ownerId) {
        return new PatchBulletInput(
                bullet.id(),
                bullet.description(),
                bullet.budget(),
                bullet.remaining().add(refund),
                bullet.walletId()
        ).withOwnerId(ownerId);
    }
}
