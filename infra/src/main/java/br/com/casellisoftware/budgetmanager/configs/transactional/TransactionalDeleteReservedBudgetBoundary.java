package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.DeleteReservedBudgetBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeleteReservedBudgetBoundary implements DeleteReservedBudgetBoundary {

    private final DeleteReservedBudgetBoundary delegate;

    public TransactionalDeleteReservedBudgetBoundary(DeleteReservedBudgetBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id, String ownerId) {
        delegate.execute(id, ownerId);
    }
}
