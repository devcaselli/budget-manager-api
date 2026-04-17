package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional decorator for {@link PayExpenseBoundary}.
 *
 * <p>Wraps the plain use case delegate so that the entire pay-expense
 * operation — payment save, expense save, bullet save — executes inside
 * a single MongoDB multi-document transaction. Any failure after the first
 * write causes all three writes to be rolled back, preserving consistency
 * for this financial aggregate.</p>
 *
 * <p>This class lives in {@code infra} because {@code @Transactional} is a
 * Spring annotation and therefore forbidden in the {@code application} module.</p>
 */
public class TransactionalPayExpenseBoundary implements PayExpenseBoundary {

    private final PayExpenseBoundary delegate;

    public TransactionalPayExpenseBoundary(PayExpenseBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public PaymentOutput execute(PayExpenseInput input) {
        return delegate.execute(input);
    }
}
