package br.com.casellisoftware.budgetmanager.domain.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.time.YearMonth;
import java.util.Objects;

/**
 * A composite identifier for subscription charge previews.
 * This value object encapsulates the format of the preview-id string
 * so format changes remain localized and do not cascade.
 *
 * Format: {@code walletId:subscriptionId:yearMonth}
 */
public record SubscriptionChargePreviewId(String walletId, String subscriptionId, YearMonth month) {

    public SubscriptionChargePreviewId {
        Objects.requireNonNull(walletId, "walletId must not be null");
        Objects.requireNonNull(subscriptionId, "subscriptionId must not be null");
        Objects.requireNonNull(month, "month must not be null");
    }

    /**
     * Factory method to create a preview id from a wallet and subscription.
     *
     * @param wallet the wallet (provides id and effective month)
     * @param subscription the subscription (provides id)
     * @return a new SubscriptionChargePreviewId
     */
    public static SubscriptionChargePreviewId of(Wallet wallet, Subscription subscription) {
        return new SubscriptionChargePreviewId(
                wallet.getId(),
                subscription.getId(),
                wallet.getEffectiveMonth()
        );
    }

    /**
     * Returns the composite id as a string in the format: {@code walletId:subscriptionId:month}.
     *
     * @return the composite id string
     */
    public String asString() {
        return walletId + ":" + subscriptionId + ":" + month;
    }

    @Override
    public String toString() {
        return asString();
    }
}
