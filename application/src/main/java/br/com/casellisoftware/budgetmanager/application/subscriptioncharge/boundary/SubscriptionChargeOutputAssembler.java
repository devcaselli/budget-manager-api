package br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionChargePreviewId;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SubscriptionChargeOutputAssembler {

    private SubscriptionChargeOutputAssembler() {
    }

    public static SubscriptionChargeOutput from(SubscriptionCharge subscriptionCharge) {
        return new SubscriptionChargeOutput(
                subscriptionCharge.getId(),
                subscriptionCharge.getSubscriptionId(),
                subscriptionCharge.getWalletId(),
                subscriptionCharge.getMonth(),
                subscriptionCharge.getAmount().amount(),
                subscriptionCharge.getRemaining().amount(),
                subscriptionCharge.getFlag()
        );
    }

    /**
     * Builds a preview {@link SubscriptionChargeOutput} for a subscription that has not
     * yet been materialized as a persisted charge. The {@code id} is a composite key
     * of the form {@code "<walletId>:<subscriptionId>:<effectiveMonth>"} so that preview
     * entries are distinguishable from real charges without a separate type field.
     */
    public static SubscriptionChargeOutput preview(Wallet wallet, Subscription subscription) {
        Money amount = subscription.resolveAmount(wallet.getEffectiveMonth());
        String previewId = SubscriptionChargePreviewId.of(wallet, subscription).asString();
        return new SubscriptionChargeOutput(
                previewId,
                subscription.getId(),
                wallet.getId(),
                wallet.getEffectiveMonth(),
                amount.amount(),
                amount.amount(),
                subscription.getFlag()
        );
    }

    /**
     * Wallet-less preview, used by views (credit-card charges) that are
     * scoped to a month rather than a wallet. {@code walletId} is null in the
     * output; the preview id stays composite so the front can still tell
     * previews apart.
     */
    public static SubscriptionChargeOutput preview(Subscription subscription,
                                                   java.time.YearMonth month) {
        return preview(subscription, month, null);
    }

    public static SubscriptionChargeOutput preview(Subscription subscription,
                                                   java.time.YearMonth month,
                                                   Share activeShare) {
        Money amount = subscription.resolveAmount(month);
        BigDecimal effectiveAmount = activeShare == null
                ? amount.amount()
                : amount.amount().multiply(activeShare.getOwnerRatio()).setScale(2, RoundingMode.HALF_EVEN);
        String previewId = subscription.getId() + ":" + month;
        return new SubscriptionChargeOutput(
                previewId,
                subscription.getId(),
                null,
                month,
                effectiveAmount,
                effectiveAmount,
                subscription.getFlag()
        );
    }
}
