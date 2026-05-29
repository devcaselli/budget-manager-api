package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.YearMonth;
import java.util.Objects;

public record SubscriptionVersion(YearMonth effectiveMonth, Money amount) {

    public SubscriptionVersion {
        Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
