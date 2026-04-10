package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

/**
 * Converts a rich-domain {@link Wallet} into the framework-agnostic
 * {@link WalletOutput} consumed by interface adapters.
 *
 * <p>Lives in the application layer (not in a use case, not in the domain)
 * because:
 * <ul>
 *   <li>Use cases must not own mapping — that's an SRP violation and guarantees
 *       duplication across use cases.</li>
 *   <li>The domain must not know about boundary DTOs — that would invert the
 *       dependency direction.</li>
 * </ul>
 *
 * <p>Hand-written on purpose: the flatten {@code Money → BigDecimal} is trivial,
 * and forcing MapStruct here would require {@code expression} attributes that
 * are uglier than the straight code below.</p>
 */
public final class WalletOutputAssembler {

    private WalletOutputAssembler() {
    }

    public static WalletOutput from(Wallet wallet) {
        return new WalletOutput(
                wallet.getId(),
                wallet.getDescription(),
                wallet.getBudget().amount(),
                wallet.getRemaining().amount(),
                wallet.getStartDate(),
                wallet.getClosedDate(),
                wallet.getClosed()
        );
    }
}