package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindAllBulletsByIdsUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.PatchBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeleteAllPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindAllPaymentByExpenseIdUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

public class DeleteExpenseByIdUseCase implements DeleteExpenseByIdBoundary {

    private final ExpenseRepository expenseRepository;
    private final FindExpenseByIdUseCase findExpenseByIdUseCase;
    private final FindAllPaymentByExpenseIdUseCase  findAllPaymentByExpenseIdUseCase;
    private final FindAllBulletsByIdsUseCase findAllBulletsByIdsUseCase;
    private final PatchBulletUseCase patchBulletUseCase;
    private final DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase;

    public DeleteExpenseByIdUseCase(ExpenseRepository expenseRepository, FindExpenseByIdUseCase findExpenseByIdUseCase, FindAllPaymentByExpenseIdUseCase findAllPaymentByExpenseIdUseCase, FindAllBulletsByIdsUseCase findAllBulletsByIdsUseCase, PatchBulletUseCase patchBulletUseCase, DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase) {
        this.expenseRepository = expenseRepository;
        this.findExpenseByIdUseCase = findExpenseByIdUseCase;
        this.findAllPaymentByExpenseIdUseCase = findAllPaymentByExpenseIdUseCase;
        this.findAllBulletsByIdsUseCase = findAllBulletsByIdsUseCase;
        this.patchBulletUseCase = patchBulletUseCase;
        this.deleteAllPaymentByIdUseCase = deleteAllPaymentByIdUseCase;
    }

    public void execute(String id){
        ExpenseOutput expense =  findExpenseByIdUseCase.execute(id);
        this.rechargeableBullets(expense.id());
        this.expenseRepository.deleteById(expense.id());
    }

    private void rechargeableBullets(String id){
        List<PaymentOutput> payments = this.findAllPaymentByExpenseIdUseCase.execute(id);

        List<BulletOutput> bullets = this.findAllBulletsByIdsUseCase.execute(
                payments.stream()
                        .map(PaymentOutput::bulletId)
                        .toList()
        );

        for(BulletOutput bullet : bullets){
            List<PaymentOutput> p = payments
                    .stream()
                    .filter(data -> data.bulletId().equals(bullet.id()))
                    .toList();


            PatchBulletInput patchBulletInput = bulletOutputToInput(
                    bullet,
                    p.stream().map(PaymentOutput::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
            );

            this.patchBulletUseCase.execute(patchBulletInput);
            this.deleteAllPaymentByIdUseCase.execute(payments.stream().map(PaymentOutput::id).collect(Collectors.toList()));
        }
    }

    private PatchBulletInput bulletOutputToInput(BulletOutput bullet, BigDecimal newRemaining){
        return new PatchBulletInput(
                bullet.id(),
                bullet.description(),
                bullet.budget(),
                bullet.remaining().add(newRemaining)
        );
    }


}
