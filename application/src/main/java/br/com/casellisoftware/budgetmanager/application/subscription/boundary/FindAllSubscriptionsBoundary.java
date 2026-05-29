package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

public interface FindAllSubscriptionsBoundary {

    PageResult<SubscriptionOutput> execute(int page, int size, String ownerId);
}
