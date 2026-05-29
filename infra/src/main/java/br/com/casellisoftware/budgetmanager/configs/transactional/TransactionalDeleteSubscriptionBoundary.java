package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeleteSubscriptionBoundary implements DeleteSubscriptionBoundary {

    private final DeleteSubscriptionBoundary delegate;

    public TransactionalDeleteSubscriptionBoundary(DeleteSubscriptionBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id, String ownerId) {
        delegate.execute(id, ownerId);
    }
}
