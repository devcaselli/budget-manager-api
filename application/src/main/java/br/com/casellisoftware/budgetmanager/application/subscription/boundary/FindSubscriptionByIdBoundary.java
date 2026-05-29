package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

public interface FindSubscriptionByIdBoundary {

    SubscriptionOutput execute(String id, String ownerId);
}
