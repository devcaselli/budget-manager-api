package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.DeleteInstallmentBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeleteInstallmentBoundary implements DeleteInstallmentBoundary {

    private final DeleteInstallmentBoundary delegate;

    public TransactionalDeleteInstallmentBoundary(DeleteInstallmentBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id, String ownerId) {
        delegate.execute(id, ownerId);
    }
}
