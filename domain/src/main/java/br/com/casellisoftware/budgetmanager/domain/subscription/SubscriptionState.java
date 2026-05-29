package br.com.casellisoftware.budgetmanager.domain.subscription;

public enum SubscriptionState {
    PRODUCTION,
    PREVIEW;

    public boolean isPreview() {
        return this == PREVIEW;
    }
}
