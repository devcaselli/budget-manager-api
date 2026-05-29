package br.com.casellisoftware.budgetmanager.domain.wallet.policy;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletEffectiveMonthConflictException;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Ensures only one open PRODUCTION wallet exists for any given
 * {@code effectiveMonth}. PREVIEW and REVIEW wallets are unconstrained.
 */
public final class WalletUniquenessPolicy {

    private WalletUniquenessPolicy() {
    }

    public static void validate(WalletRepository walletRepository, Wallet wallet, Clock clock) {
        Objects.requireNonNull(walletRepository, "walletRepository must not be null");
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(clock, "clock must not be null");

        if (wallet.getState() != WalletState.PRODUCTION) {
            return;
        }
        if (Boolean.TRUE.equals(wallet.getClosed())) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (walletRepository.existsOpenProductionFor(wallet.getEffectiveMonth(), today, wallet.getId(), wallet.getOwnerId())) {
            throw new WalletEffectiveMonthConflictException(wallet.getEffectiveMonth());
        }
    }
}
