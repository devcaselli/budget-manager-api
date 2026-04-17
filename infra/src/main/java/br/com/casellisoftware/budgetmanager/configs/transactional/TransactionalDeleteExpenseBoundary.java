package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transactional decorator for {@link DeleteExpenseByIdBoundary}.
 *
 * <p>Wraps the plain use case delegate so that the entire delete-expense
 * operation — bullet patches (refund), payment deletions, expense deletion —
 * executes inside a single MongoDB multi-document transaction. Any failure
 * after the first write causes all writes to be rolled back, preserving
 * financial consistency.</p>
 *
 * <p>This class lives in {@code infra} because {@code @Transactional} is a
 * Spring annotation and therefore forbidden in the {@code application} module.</p>
 */
public class TransactionalDeleteExpenseBoundary implements DeleteExpenseByIdBoundary {

    private final DeleteExpenseByIdBoundary delegate;

    public TransactionalDeleteExpenseBoundary(DeleteExpenseByIdBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id) {
        delegate.execute(id);
    }
}
