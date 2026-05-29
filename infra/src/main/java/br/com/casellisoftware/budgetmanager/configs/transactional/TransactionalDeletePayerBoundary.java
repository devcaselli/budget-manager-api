package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.DeletePayerByIdBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeletePayerBoundary implements DeletePayerByIdBoundary {

    private final DeletePayerByIdBoundary delegate;

    public TransactionalDeletePayerBoundary(DeletePayerByIdBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id, String ownerId) {
        delegate.execute(id, ownerId);
    }
}
