package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;

public final class SubscriptionOutputAssembler {

    private SubscriptionOutputAssembler() {
    }

    public static SubscriptionOutput from(Subscription subscription) {
        return new SubscriptionOutput(
                subscription.getId(),
                subscription.getDescription(),
                subscription.getCurrency().getCurrencyCode(),
                subscription.getState().name(),
                subscription.getStartMonth(),
                subscription.getEndMonth(),
                subscription.getVersions().stream()
                        .map(SubscriptionOutputAssembler::fromVersion)
                        .toList(),
                subscription.getFlag(),
                subscription.getCreditCardId()
        );
    }

    private static SubscriptionVersionOutput fromVersion(SubscriptionVersion version) {
        return new SubscriptionVersionOutput(
                version.effectiveMonth(),
                version.amount().amount()
        );
    }
}
