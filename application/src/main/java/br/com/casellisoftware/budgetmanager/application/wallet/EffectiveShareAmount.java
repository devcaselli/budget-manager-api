package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;

/**
 * Shared computation helpers for post-share effective amounts.
 *
 * <p>Both wallet calculators ({@link SubscriptionWalletBalanceCalculator},
 * {@link InstallmentWalletBalanceCalculator}) and the cap validation path
 * ({@code ReservedBudgetLinkValidationService}) must produce identical numbers.
 * Extracting the computation here avoids drift between the two paths.</p>
 *
 * <p>Scale and rounding rules mirror the wallet calculators exactly:</p>
 * <ul>
 *   <li>Subscription: scale 2, {@link RoundingMode#HALF_EVEN}</li>
 *   <li>Installment: preserves the installment value's own scale,
 *       {@link RoundingMode#HALF_EVEN}</li>
 * </ul>
 */
public final class EffectiveShareAmount {

    private EffectiveShareAmount() {
    }

    /**
     * Returns the post-share effective amount for a subscription in {@code month}.
     * Mirrors the private {@code applyOwnerRatio} method in
     * {@link SubscriptionWalletBalanceCalculator}.
     *
     * @param subscription the subscription (never {@code null})
     * @param activeShare  the active share for this subscription, or {@code null} if none
     * @param month        the month being evaluated
     * @return the owner's portion of the subscription amount for that month
     * @implNote Time complexity: O(V) where V = number of subscription versions (binary
     *     search via resolveAmount). Space: O(1).
     */
    public static Money forSubscription(Subscription subscription, Share activeShare, YearMonth month) {
        Money amount = subscription.resolveAmount(month);
        if (activeShare == null || !activeShare.isEffectiveFor(month)) {
            return amount;
        }
        BigDecimal scaled = amount.amount()
                .multiply(activeShare.getOwnerRatio())
                .setScale(Money.SCALE, RoundingMode.HALF_EVEN);
        return Money.of(scaled, amount.currency());
    }

    /**
     * Returns the post-share effective amount for an installment in {@code month}.
     * Mirrors the private {@code effectiveValue} method in
     * {@link InstallmentWalletBalanceCalculator}.
     *
     * @param installment the installment (never {@code null})
     * @param activeShare the active share for this installment, or {@code null} if none
     * @param month       the month being evaluated (used for share gate only)
     * @return the owner's portion of the installment value
     * @implNote Scale is preserved from the installment value (not forced to 2) to keep
     *     rounding behaviour identical to the wallet calculator.
     */
    public static Money forInstallment(Installment installment, Share activeShare, YearMonth month) {
        Money value = installment.installmentValue();
        if (activeShare == null || !activeShare.isEffectiveFor(month)) {
            return value;
        }
        BigDecimal scaled = value.amount()
                .multiply(activeShare.getOwnerRatio())
                .setScale(value.amount().scale(), RoundingMode.HALF_EVEN);
        return Money.of(scaled, value.currency());
    }
}
