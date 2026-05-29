package br.com.casellisoftware.budgetmanager.domain.wallet.policy;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;

import java.math.BigDecimal;
import java.util.Objects;

public final class BulletAllocationPolicy {

    private BulletAllocationPolicy() {
    }

    public static void validateAllocation(Wallet wallet, Money requestedBudget) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(requestedBudget, "requestedBudget must not be null");

        requireSameCurrency(wallet, requestedBudget);
        validateAllocation(wallet, requestedBudget, Money.of(BigDecimal.ZERO, wallet.getBudget().currency()));
    }

    public static void validateAllocation(Wallet wallet, Money requestedBudget, Money reservedBudget) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(requestedBudget, "requestedBudget must not be null");
        Objects.requireNonNull(reservedBudget, "reservedBudget must not be null");

        requireSameCurrency(wallet, requestedBudget);
        requireSameCurrency(wallet, reservedBudget);

        Money required = requestedBudget.add(reservedBudget);
        if (requestedBudget.isGreaterThan(wallet.getRemaining())) {
            throw new WalletAllocationExceededException(
                    "Requested bullet budget " + requestedBudget.amount()
                            + " exceeds wallet remaining " + wallet.getRemaining().amount()
                            + " (walletId=" + wallet.getId() + ")");
        }
        if (required.isGreaterThan(wallet.getRemaining())) {
            throw new WalletAllocationExceededException(
                    "Requested bullet budget " + requestedBudget.amount()
                            + " plus reserved subscriptions " + reservedBudget.amount()
                            + " exceeds wallet remaining " + wallet.getRemaining().amount()
                            + " (walletId=" + wallet.getId() + ")");
        }
    }

    public static void validateReallocation(Wallet wallet, Money currentBudget, Money newBudget, Money currentBulletRemaining) {
        validateReallocation(wallet, currentBudget, newBudget, currentBulletRemaining,
                Money.of(BigDecimal.ZERO, wallet.getBudget().currency()));
    }

    public static void validateReallocation(Wallet wallet,
                                            Money currentBudget,
                                            Money newBudget,
                                            Money currentBulletRemaining,
                                            Money reservedBudget) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(currentBudget, "currentBudget must not be null");
        Objects.requireNonNull(newBudget, "newBudget must not be null");
        Objects.requireNonNull(currentBulletRemaining, "currentBulletRemaining must not be null");
        Objects.requireNonNull(reservedBudget, "reservedBudget must not be null");

        requireSameCurrency(wallet, newBudget);
        requireSameCurrency(wallet, reservedBudget);

        Money consumed = currentBudget.subtract(currentBulletRemaining);
        if (consumed.isGreaterThan(newBudget)) {
            throw new WalletAllocationExceededException(
                    "New bullet budget " + newBudget.amount()
                            + " is below already consumed amount " + consumed.amount());
        }

        if (newBudget.isGreaterThan(currentBudget)) {
            Money delta = newBudget.subtract(currentBudget);
            Money required = delta.add(reservedBudget);
            if (required.isGreaterThan(wallet.getRemaining())) {
                throw new WalletAllocationExceededException(
                        "Bullet budget increase of " + delta.amount()
                                + " plus reserved subscriptions " + reservedBudget.amount()
                                + " exceeds wallet remaining " + wallet.getRemaining().amount());
            }
        }
    }

    private static void requireSameCurrency(Wallet wallet, Money money) {
        if (!wallet.getBudget().currency().equals(money.currency())) {
            throw new WalletCurrencyMismatchException(
                    "Currency mismatch: wallet=" + wallet.getBudget().currency()
                            + " requested=" + money.currency());
        }
    }
}
