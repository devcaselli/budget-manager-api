package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;

public interface FindSubscriptionDomainByIdBoundary {

    Subscription findById(String id, String ownerId);
}
