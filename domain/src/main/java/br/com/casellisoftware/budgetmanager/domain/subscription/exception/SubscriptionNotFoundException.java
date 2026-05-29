package br.com.casellisoftware.budgetmanager.domain.subscription.exception;

public class SubscriptionNotFoundException extends RuntimeException {

    public SubscriptionNotFoundException(String id) {
        super("Subscription not found: " + id);
    }
}
