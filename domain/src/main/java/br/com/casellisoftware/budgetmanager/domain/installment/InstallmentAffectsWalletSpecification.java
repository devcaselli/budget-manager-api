package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Encapsulates the rule "does this installment affect this wallet?"
 */
public final class InstallmentAffectsWalletSpecification {

    private InstallmentAffectsWalletSpecification() {
    }

    public static boolean isSatisfiedBy(Installment installment, Wallet wallet) {
        Objects.requireNonNull(installment, "installment must not be null");
        Objects.requireNonNull(wallet, "wallet must not be null");
        return isSatisfiedBy(installment, wallet.getEffectiveMonth());
    }

    public static boolean isSatisfiedBy(Installment installment, YearMonth walletMonth) {
        Objects.requireNonNull(installment, "installment must not be null");
        Objects.requireNonNull(walletMonth, "walletMonth must not be null");
        if (installment.isDeleted()) {
            return false;
        }
        // Standalone installments (no source wallet) charge their first parcel in
        // `sourceEffectiveMonth` itself, so the source month is inclusive.
        // From-expense installments already counted the purchase in the source
        // wallet's month — their first parcel materializes the month after, so
        // the source month is excluded for them.
        YearMonth source = installment.getSourceEffectiveMonth();
        boolean sourceMonthInclusive = installment.getSourceWalletId() == null;
        boolean afterSource = sourceMonthInclusive
                ? !source.isAfter(walletMonth)
                : source.isBefore(walletMonth);
        return afterSource && !installment.getLastInstallmentDate().isBefore(walletMonth);
    }
}
