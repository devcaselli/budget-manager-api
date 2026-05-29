package br.com.casellisoftware.budgetmanager.domain.subscription.exception;

public class SubscriptionAlreadyEndedException extends RuntimeException {

    public SubscriptionAlreadyEndedException(String id) {
        super("Subscription already ended: " + id);
    }
}
