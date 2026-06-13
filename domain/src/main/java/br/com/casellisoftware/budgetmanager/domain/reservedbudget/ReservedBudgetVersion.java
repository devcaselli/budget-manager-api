package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.YearMonth;
import java.util.Objects;

/**
 * A single point in a {@link ReservedBudget}'s variation history.
 *
 * <p>The reserved amount becomes {@code amount} from {@code effectiveMonth} onward
 * until a later version supersedes it. Mirrors
 * {@code br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion}.</p>
 */
public record ReservedBudgetVersion(YearMonth effectiveMonth, Money amount) {

    public ReservedBudgetVersion {
        Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
