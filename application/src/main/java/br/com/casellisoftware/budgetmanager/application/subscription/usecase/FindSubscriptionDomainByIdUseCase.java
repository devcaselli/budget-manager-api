package br.com.casellisoftware.budgetmanager.application.subscription.usecase;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindSubscriptionDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;

public class FindSubscriptionDomainByIdUseCase implements FindSubscriptionDomainByIdBoundary {

    private final SubscriptionRepository subscriptionRepository;

    public FindSubscriptionDomainByIdUseCase(SubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Subscription findById(String id, String ownerId) {
        return subscriptionRepository.findById(id, ownerId)
                .orElseThrow(() -> new SubscriptionNotFoundException(id));
    }
}
