package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveSubscriptionBoundary implements SaveSubscriptionBoundary {

    private final SaveSubscriptionBoundary delegate;

    public TransactionalSaveSubscriptionBoundary(SaveSubscriptionBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public SubscriptionOutput execute(SubscriptionInput input) {
        return delegate.execute(input);
    }
}
