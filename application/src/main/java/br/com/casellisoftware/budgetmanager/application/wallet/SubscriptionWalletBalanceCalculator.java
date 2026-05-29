package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SubscriptionWalletBalanceCalculator {

    private SubscriptionWalletBalanceCalculator() {
    }

    public static Money subscriptionTotal(Wallet wallet, List<Subscription> subscriptions) {
        return subscriptionTotal(wallet, subscriptions, null);
    }

    /**
     * Batch-aware overload. Fetches all active shares in a single repository call
     * before iterating so the loop runs in O(1) DB queries (one batch), not O(n).
     */
    public static Money subscriptionTotal(Wallet wallet,
                                          List<Subscription> subscriptions,
                                          ShareRepository shareRepository) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(subscriptions, "subscriptions must not be null");
        Currency walletCurrency = wallet.getBudget().currency();
        YearMonth month = wallet.getEffectiveMonth();

        if (subscriptions.isEmpty()) {
            return Money.of(BigDecimal.ZERO, walletCurrency);
        }

        Map<String, Share> activeShares = (shareRepository != null)
                ? shareRepository.findActiveBySourceIds(
                        ShareSourceType.SUBSCRIPTION,
                        subscriptions.stream().map(Subscription::getId).toList(),
                        wallet.getOwnerId())
                : Map.of();

        Money total = Money.of(BigDecimal.ZERO, walletCurrency);
        for (Subscription subscription : subscriptions) {
            Money amount = subscription.resolveAmount(month);
            if (!walletCurrency.equals(amount.currency())) {
                throw new WalletCurrencyMismatchException(
                        "Currency mismatch: wallet=" + walletCurrency
                                + " subscription=" + amount.currency()
                                + " (walletId=" + wallet.getId()
                                + ", subscriptionId=" + subscription.getId() + ")");
            }
            total = total.add(applyOwnerRatio(amount, activeShares.get(subscription.getId())));
        }
        return total;
    }

    public static BigDecimal effectiveRemainingAmount(Wallet wallet, List<Subscription> subscriptions) {
        return effectiveRemainingAmount(wallet, subscriptions, null);
    }

    public static BigDecimal effectiveRemainingAmount(Wallet wallet,
                                                      List<Subscription> subscriptions,
                                                      ShareRepository shareRepository) {
        Money subscriptionTotal = subscriptionTotal(wallet, subscriptions, shareRepository);
        return wallet.getRemaining().amount().subtract(subscriptionTotal.amount());
    }

    private static Money applyOwnerRatio(Money amount, Share activeShare) {
        if (activeShare == null) {
            return amount;
        }
        BigDecimal scaled = amount.amount().multiply(activeShare.getOwnerRatio()).setScale(2, RoundingMode.HALF_EVEN);
        return Money.of(scaled, amount.currency());
    }
}
