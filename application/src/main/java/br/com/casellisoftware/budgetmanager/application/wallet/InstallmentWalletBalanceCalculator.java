package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InstallmentWalletBalanceCalculator {

    private InstallmentWalletBalanceCalculator() {
    }

    public static Money installmentTotal(Wallet wallet, List<Installment> installments) {
        return installmentTotal(wallet, installments, null);
    }

    /**
     * Batch-aware overload. Fetches all active shares in a single repository call
     * before iterating so the loop runs in O(n) DB queries = O(1) (one batch query),
     * not O(n) (one query per installment).
     */
    public static Money installmentTotal(Wallet wallet,
                                         List<Installment> installments,
                                         ShareRepository shareRepository) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(installments, "installments must not be null");
        Currency walletCurrency = wallet.getBudget().currency();

        List<Installment> relevant = installments.stream()
                .filter(i -> InstallmentAffectsWalletSpecification.isSatisfiedBy(i, wallet))
                .toList();

        if (relevant.isEmpty()) {
            return Money.of(BigDecimal.ZERO, walletCurrency);
        }

        Map<String, Share> activeShares = (shareRepository != null)
                ? shareRepository.findActiveBySourceIds(
                        ShareSourceType.INSTALLMENT,
                        relevant.stream().map(Installment::getId).toList(),
                        wallet.getOwnerId())
                : Map.of();

        Money total = Money.of(BigDecimal.ZERO, walletCurrency);
        for (Installment installment : relevant) {
            if (!walletCurrency.equals(installment.getInstallmentValue().currency())) {
                throw new WalletCurrencyMismatchException(
                        "Currency mismatch: wallet=" + walletCurrency
                                + " installment=" + installment.getInstallmentValue().currency()
                                + " (walletId=" + wallet.getId()
                                + ", installmentId=" + installment.getId() + ")"
                );
            }
            total = total.add(effectiveValue(installment, walletCurrency,
                    activeShares.get(installment.getId()), wallet.getEffectiveMonth()));
        }
        return total;
    }

    private static Money effectiveValue(Installment installment, Currency currency, Share activeShare,
                                        java.time.YearMonth walletMonth) {
        if (activeShare == null || !activeShare.isEffectiveFor(walletMonth)) {
            return installment.getInstallmentValue();
        }
        BigDecimal scaled = installment.getInstallmentValue().amount()
                .multiply(activeShare.getOwnerRatio())
                .setScale(installment.getInstallmentValue().amount().scale(), RoundingMode.HALF_EVEN);
        return Money.of(scaled, currency);
    }
}
