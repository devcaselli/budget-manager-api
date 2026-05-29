package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

import java.util.List;
import java.util.Objects;

public final class WalletSubscriptionSelector {

    private WalletSubscriptionSelector() {
    }

    public static List<Subscription> activeForWallet(SubscriptionRepository repository, Wallet wallet) {
        Objects.requireNonNull(repository, "repository must not be null");
        Objects.requireNonNull(wallet, "wallet must not be null");
        SubscriptionState subscriptionState = wallet.getState() == WalletState.PREVIEW
                ? SubscriptionState.PREVIEW
                : SubscriptionState.PRODUCTION;
        return repository.findActiveFor(wallet.getEffectiveMonth(), subscriptionState, wallet.getOwnerId());
    }
}
