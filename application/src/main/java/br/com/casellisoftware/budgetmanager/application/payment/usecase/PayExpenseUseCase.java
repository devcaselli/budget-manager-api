package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.policy.PaymentPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers a payment against an expense and its matching bullet.
 *
 * <p>Coordinates three aggregates in-process: persists the payment first,
 * then debits the expense and the bullet via their domain behaviors
 * ({@link Expense#pay(Payment)}, {@link Bullet#pay(Payment)}) and persists
 * the new immutable instances. Works with domain entities directly via
 * their repository ports — no round-trip through Output DTOs.</p>
 */
public class PayExpenseUseCase implements PayExpenseBoundary {

    private static final Logger log = LoggerFactory.getLogger(PayExpenseUseCase.class);

    private final PaymentRepository paymentRepository;
    private final ExpenseRepository expenseRepository;
    private final BulletRepository bulletRepository;

    public PayExpenseUseCase(PaymentRepository paymentRepository,
                             ExpenseRepository expenseRepository,
                             BulletRepository bulletRepository) {
        this.paymentRepository = paymentRepository;
        this.expenseRepository = expenseRepository;
        this.bulletRepository = bulletRepository;
    }

    @Override
    public PaymentOutput execute(PayExpenseInput input) {
        log.info("Paying expenseId={} via bulletId={}", input.expenseId(), input.bulletId());

        Expense expense = expenseRepository.findById(input.expenseId())
                .orElseThrow(() -> new ExpenseNotFoundException(input.expenseId()));
        Bullet bullet = bulletRepository.findById(input.bulletId())
                .orElseThrow(() -> new BulletNotFoundException("Bullet not found: " + input.bulletId()));

        PaymentPolicy.validate(expense, bullet, input.amount(), input.walletId());

        Payment savedPayment = paymentRepository.save(Payment.create(
                input.amount(),
                input.paymentDate(),
                input.details(),
                input.expenseId(),
                input.walletId(),
                input.bulletId()
        ));

        expenseRepository.save(expense.pay(savedPayment));
        bulletRepository.save(bullet.pay(savedPayment));

        log.info("Payment registered, paymentId={}", savedPayment.getId());
        return PaymentOutputAssembler.from(savedPayment);
    }
}
