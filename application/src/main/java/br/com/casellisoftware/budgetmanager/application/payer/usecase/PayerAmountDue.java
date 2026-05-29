package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Objects;

/**
 * Two-axis breakdown of what a payer owes across active shares.
 *
 * <ul>
 *   <li>{@code monthly} — what the payer pays in the current wallet/month.
 *   For expense shares: the whole quota (one-shot). For installment shares:
 *   one parcel ({@code installmentValue * quota.ratio}). For subscription
 *   shares: the recurring monthly amount ({@code subscriptionAmount * quota.ratio}).
 *   </li>
 *   <li>{@code journey} — total exposure across the lifetime of the shares.
 *   For expense shares: same as monthly. For installment shares: the entire
 *   debt ({@code share.totalAmount * quota.ratio} = installment originalValue
 *   times ratio). For subscription shares: equals monthly because the
 *   subscription has no fixed end (until the share is reverted).
 *   </li>
 * </ul>
 *
 * <p>Both values share the share's currency. Mixing currencies across
 * shares of the same payer raises {@link IllegalStateException}.</p>
 */
public record PayerAmountDue(Money monthly, Money journey) {

    public PayerAmountDue {
        Objects.requireNonNull(monthly, "monthly must not be null");
        Objects.requireNonNull(journey, "journey must not be null");
    }

    public static PayerAmountDue zero() {
        return new PayerAmountDue(Money.zero(), Money.zero());
    }
}
