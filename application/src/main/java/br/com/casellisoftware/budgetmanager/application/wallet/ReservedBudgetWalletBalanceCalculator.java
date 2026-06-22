package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Sums the reserved-budget amounts effective for a wallet's month. Each reserved budget
 * resolves its month-effective amount via {@link ReservedBudget#resolveAmount(YearMonth)}.
 * Unlike subscriptions, reserved budgets carry no sharing, so amounts are summed verbatim.
 */
public final class ReservedBudgetWalletBalanceCalculator {

    private ReservedBudgetWalletBalanceCalculator() {
    }

    public static Money reservedBudgetTotal(Wallet wallet, List<ReservedBudget> reservedBudgets) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(reservedBudgets, "reservedBudgets must not be null");
        Currency walletCurrency = wallet.getBudget().currency();
        YearMonth month = wallet.getEffectiveMonth();

        if (reservedBudgets.isEmpty()) {
            return Money.of(BigDecimal.ZERO, walletCurrency);
        }

        Money total = Money.of(BigDecimal.ZERO, walletCurrency);
        for (ReservedBudget reservedBudget : reservedBudgets) {
            Money amount = reservedBudget.resolveAmount(month);
            if (!walletCurrency.equals(amount.currency())) {
                throw new WalletCurrencyMismatchException(
                        "Currency mismatch: wallet=" + walletCurrency
                                + " reservedBudget=" + amount.currency()
                                + " (walletId=" + wallet.getId()
                                + ", reservedBudgetId=" + reservedBudget.getId() + ")");
            }
            total = total.add(amount);
        }
        return total;
    }
}
