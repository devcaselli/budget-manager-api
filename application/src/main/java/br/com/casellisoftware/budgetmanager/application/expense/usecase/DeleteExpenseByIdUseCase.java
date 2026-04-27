package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteExpenseByIdUseCase implements DeleteExpenseByIdBoundary {

    private final ExpenseRepository expenseRepository;
    private final FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary;
    private final FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary;
    private final PatchBulletBoundary patchBulletBoundary;
    private final DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary;

    public DeleteExpenseByIdUseCase(ExpenseRepository expenseRepository,
                                    FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary,
                                    FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary,
                                    PatchBulletBoundary patchBulletBoundary,
                                    DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary) {
        this.expenseRepository = expenseRepository;
        this.findAllPaymentByExpenseIdBoundary = findAllPaymentByExpenseIdBoundary;
        this.findAllBulletsByIdsBoundary = findAllBulletsByIdsBoundary;
        this.patchBulletBoundary = patchBulletBoundary;
        this.deleteAllPaymentByIdBoundary = deleteAllPaymentByIdBoundary;
    }

    public void execute(String id) {
        if (!expenseRepository.existsById(id)) {
            throw new ExpenseNotFoundException(id);
        }

        rechargeableBullets(id);
        expenseRepository.deleteById(id);
    }

    private void rechargeableBullets(String expenseId) {
        List<PaymentOutput> payments = findAllPaymentByExpenseIdBoundary.execute(expenseId);
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
                        toPatchInput(bullet, refundsByBulletId.get(bullet.id()))
                ));

        deleteAllPaymentByIdBoundary.execute(
                payments.stream().map(PaymentOutput::id).toList()
        );
    }

    private PatchBulletInput toPatchInput(BulletOutput bullet, BigDecimal refund) {
        return new PatchBulletInput(
                bullet.id(),
                bullet.description(),
                bullet.budget(),
                bullet.remaining().add(refund),
                bullet.walletId()
        );
    }
}
