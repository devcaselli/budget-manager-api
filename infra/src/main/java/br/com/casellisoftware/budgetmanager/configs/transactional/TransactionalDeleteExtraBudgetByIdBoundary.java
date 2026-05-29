package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.DeleteExtraBudgetByIdBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeleteExtraBudgetByIdBoundary implements DeleteExtraBudgetByIdBoundary {

    private final DeleteExtraBudgetByIdBoundary delegate;

    public TransactionalDeleteExtraBudgetByIdBoundary(DeleteExtraBudgetByIdBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id, String ownerId) {
        delegate.execute(id, ownerId);
    }
}
