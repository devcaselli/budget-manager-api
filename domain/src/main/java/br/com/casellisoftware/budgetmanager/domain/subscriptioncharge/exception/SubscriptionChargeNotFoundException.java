package br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.exception;

public class SubscriptionChargeNotFoundException extends RuntimeException {

    public SubscriptionChargeNotFoundException(String id) {
        super("Subscription charge not found: " + id);
    }
}
