package br.com.casellisoftware.budgetmanager.service;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.PatchExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.PatchExpenseInput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final FindExpenseByIdBoundary findExpenseByIdBoundary;
    private final FindBulletByIdBoundary findBulletByIdBoundary;
    private final PatchExpenseBoundary patchExpenseBoundary;
    private final PatchBulletBoundary patchBulletBoundary;

    public void pay(PayRequestDto request, String walletId) {
        Expense expense = toExpense(findExpenseByIdBoundary.execute(request.expenseId()));
        Bullet bullet = toBullet(findBulletByIdBoundary.execute(request.bulletId()));

        Payment savedPayment = paymentRepository.save(createPayment(request, walletId));

        Expense paidExpense = expense.pay(savedPayment);
        Bullet paidBullet = bullet.pay(savedPayment);
        patchExpenseBoundary.execute(toPatchExpenseInput(paidExpense));
        patchBulletBoundary.execute(toPatchBulletInput(paidBullet));

    }

    private Expense toExpense(ExpenseOutput output) {
        return new Expense(
                output.id(),
                output.walletId(),
                output.name(),
                Money.of(output.cost()),
                Money.of(output.remaining()),
                output.purchaseDate(),
                output.paymentIds()
        );
    }

    private Bullet toBullet(BulletOutput output) {
        return new Bullet(
                output.id(),
                output.description(),
                Money.of(output.budget()),
                Money.of(output.remaining()),
                output.walletId()
        );
    }

    private Payment createPayment(PayRequestDto dto, String walletId) {
        return Payment.create(
                dto.payment().amount(),
                dto.payment().paymentDate(),
                dto.payment().details(),
                dto.expenseId(),
                walletId,
                dto.bulletId()
        );
    }

    private PatchExpenseInput toPatchExpenseInput(Expense expense) {
        return new PatchExpenseInput(
                expense.getId(),
                expense.getName(),
                expense.getCost().amount(),
                expense.getRemaining().amount(),
                expense.getPurchaseDate(),
                expense.getPaymentIds()
        );
    }

    private PatchBulletInput toPatchBulletInput(Bullet bullet) {
        return new PatchBulletInput(
                bullet.getId(),
                bullet.getDescription(),
                bullet.getBudget().amount(),
                bullet.getRemaining().amount()
        );
    }
}
