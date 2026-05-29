package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.RevertShareBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalRevertShareBoundary implements RevertShareBoundary {

    private final RevertShareBoundary delegate;

    public TransactionalRevertShareBoundary(RevertShareBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String shareId, String ownerId) {
        delegate.execute(shareId, ownerId);
    }
}
