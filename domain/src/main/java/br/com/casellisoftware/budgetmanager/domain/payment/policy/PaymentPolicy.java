package br.com.casellisoftware.budgetmanager.domain.payment.policy;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.payment.AmountExceedsRemainingException;
import br.com.casellisoftware.budgetmanager.domain.payment.CurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.domain.payment.WalletMismatchException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Objects;

public final class PaymentPolicy {

    private PaymentPolicy() {
    }

    public static void validate(Expense expense, Bullet bullet, Money paymentAmount, String inputWalletId) {
        Objects.requireNonNull(expense, "expense must not be null");
        Objects.requireNonNull(bullet, "bullet must not be null");
        Objects.requireNonNull(paymentAmount, "paymentAmount must not be null");
        Objects.requireNonNull(inputWalletId, "inputWalletId must not be null");

        if (!Objects.equals(expense.getWalletId(), bullet.getWalletId())) {
            throw new WalletMismatchException("Expense wallet " + expense.getWalletId()
                    + " does not match bullet wallet " + bullet.getWalletId());
        }
        if (!Objects.equals(expense.getWalletId(), inputWalletId)) {
            throw new WalletMismatchException("Input wallet " + inputWalletId
                    + " does not match expense wallet " + expense.getWalletId());
        }

        if (!expense.getCost().currency().equals(bullet.getBudget().currency())
                || !expense.getCost().currency().equals(paymentAmount.currency())) {
            throw new CurrencyMismatchException("Currency mismatch: expense=" + expense.getCost().currency()
                    + " bullet=" + bullet.getBudget().currency()
                    + " payment=" + paymentAmount.currency());
        }

        if (paymentAmount.isGreaterThan(expense.getRemaining())) {
            throw new AmountExceedsRemainingException("Payment amount exceeds expense remaining");
        }
        if (paymentAmount.isGreaterThan(bullet.getRemaining())) {
            throw new AmountExceedsRemainingException("Payment amount exceeds bullet remaining");
        }
    }
}
