package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindAllBulletsByIdsUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.PatchBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeleteAllPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeleteExpenseByIdUseCase implements DeleteExpenseByIdBoundary {

    private final ExpenseRepository expenseRepository;
    private final FindExpenseByIdUseCase findExpenseByIdUseCase;
    private final FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdUseCase;
    private final FindAllBulletsByIdsUseCase findAllBulletsByIdsUseCase;
    private final PatchBulletUseCase patchBulletUseCase;
    private final DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase;

    public DeleteExpenseByIdUseCase(ExpenseRepository expenseRepository,
                                    FindExpenseByIdUseCase findExpenseByIdUseCase,
                                    FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdUseCase,
                                    FindAllBulletsByIdsUseCase findAllBulletsByIdsUseCase,
                                    PatchBulletUseCase patchBulletUseCase,
                                    DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase) {
        this.expenseRepository = expenseRepository;
        this.findExpenseByIdUseCase = findExpenseByIdUseCase;
        this.findAllPaymentByExpenseIdUseCase = findAllPaymentByExpenseIdUseCase;
        this.findAllBulletsByIdsUseCase = findAllBulletsByIdsUseCase;
        this.patchBulletUseCase = patchBulletUseCase;
        this.deleteAllPaymentByIdUseCase = deleteAllPaymentByIdUseCase;
    }

    public void execute(String id) {
        ExpenseOutput expense = findExpenseByIdUseCase.execute(id);
        rechargeableBullets(expense.id());
        expenseRepository.deleteById(expense.id());
    }

    private void rechargeableBullets(String expenseId) {
        List<PaymentOutput> payments = findAllPaymentByExpenseIdUseCase.execute(expenseId);
        if (payments.isEmpty()) {
            return;
        }

        Map<String, BigDecimal> refundsByBulletId = payments.stream()
                .collect(Collectors.groupingBy(
                        PaymentOutput::bulletId,
                        Collectors.reducing(BigDecimal.ZERO, PaymentOutput::amount, BigDecimal::add)
                ));

        findAllBulletsByIdsUseCase.execute(List.copyOf(refundsByBulletId.keySet()))
                .forEach(bullet -> patchBulletUseCase.execute(
                        toPatchInput(bullet, refundsByBulletId.get(bullet.id()))
                ));

        deleteAllPaymentByIdUseCase.execute(
                payments.stream().map(PaymentOutput::id).toList()
        );
    }

    private PatchBulletInput toPatchInput(BulletOutput bullet, BigDecimal refund) {
        return new PatchBulletInput(
                bullet.id(),
                bullet.description(),
                bullet.budget(),
                bullet.remaining().add(refund)
        );
    }
}
