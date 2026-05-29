package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

public interface PatchSubscriptionBoundary {

    SubscriptionOutput execute(PatchSubscriptionInput input);
}
