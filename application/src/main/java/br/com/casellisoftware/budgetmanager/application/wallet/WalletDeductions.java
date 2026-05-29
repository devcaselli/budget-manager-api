package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

import java.math.BigDecimal;
import java.util.Objects;

public record WalletDeductions(Money subscriptions, Money installments) {

    public WalletDeductions {
        Objects.requireNonNull(subscriptions, "subscriptions must not be null");
        Objects.requireNonNull(installments, "installments must not be null");
    }

    public BigDecimal remainingFor(Wallet wallet) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        return wallet.getRemaining().amount()
                .subtract(subscriptions.amount())
                .subtract(installments.amount());
    }
}
