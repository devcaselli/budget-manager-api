package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductions;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.math.BigDecimal;
import java.util.List;

/**
 * Converts a rich-domain {@link Wallet} into the framework-agnostic
 * {@link WalletOutput} consumed by interface adapters.
 */
public final class WalletOutputAssembler {

    private WalletOutputAssembler() {
    }

    public static WalletOutput from(Wallet wallet) {
        return from(wallet, wallet.getRemaining().amount());
    }

    public static WalletOutput from(Wallet wallet, WalletDeductions deductions) {
        return from(wallet, deductions.remainingFor(wallet));
    }

    public static WalletOutput from(Wallet wallet, List<Subscription> activeSubscriptions) {
        BigDecimal subscriptionTotal = activeSubscriptions.stream()
                .map(subscription -> subscription.resolveAmount(wallet.getEffectiveMonth()).amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return from(wallet, wallet.getRemaining().amount().subtract(subscriptionTotal));
    }

    public static WalletOutput from(Wallet wallet,
                                    List<Subscription> activeSubscriptions,
                                    List<Installment> activeInstallments) {
        BigDecimal subscriptionTotal = activeSubscriptions.stream()
                .map(subscription -> subscription.resolveAmount(wallet.getEffectiveMonth()).amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal afterSubscriptions = wallet.getRemaining().amount().subtract(subscriptionTotal);
        BigDecimal installmentTotal = activeInstallments.stream()
                .filter(installment -> InstallmentAffectsWalletSpecification.isSatisfiedBy(installment, wallet))
                .map(installment -> installment.getInstallmentValue().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return from(wallet, afterSubscriptions.subtract(installmentTotal));
    }

    public static WalletOutput from(Wallet wallet, BigDecimal remaining) {
        return new WalletOutput(
                wallet.getId(),
                wallet.getDescription(),
                wallet.getBudget().amount(),
                remaining,
                wallet.getStartDate(),
                wallet.getClosedDate(),
                wallet.getClosed(),
                wallet.getEffectiveMonth(),
                wallet.getState(),
                wallet.getFlag()
        );
    }
}
