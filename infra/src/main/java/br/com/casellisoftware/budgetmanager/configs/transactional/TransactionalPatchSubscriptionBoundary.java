package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalPatchSubscriptionBoundary implements PatchSubscriptionBoundary {

    private final PatchSubscriptionBoundary delegate;

    public TransactionalPatchSubscriptionBoundary(PatchSubscriptionBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public SubscriptionOutput execute(PatchSubscriptionInput input) {
        return delegate.execute(input);
    }
}
