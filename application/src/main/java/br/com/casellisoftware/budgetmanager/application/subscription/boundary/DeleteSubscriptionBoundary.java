package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

public interface DeleteSubscriptionBoundary {

    void execute(String id, String ownerId);
}
