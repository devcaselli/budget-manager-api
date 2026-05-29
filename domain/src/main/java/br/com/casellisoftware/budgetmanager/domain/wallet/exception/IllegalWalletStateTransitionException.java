package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

/**
 * Thrown when a {@link br.com.casellisoftware.budgetmanager.domain.wallet.Wallet}
 * state transition violates the lifecycle rules (e.g. transitioning to REVIEW
 * while still open, or trying to leave REVIEW which is terminal).
 */
public class IllegalWalletStateTransitionException extends RuntimeException {

    public IllegalWalletStateTransitionException(WalletState from, WalletState to, String reason) {
        super("illegal wallet state transition " + from + " -> " + to + ": " + reason);
    }

    public IllegalWalletStateTransitionException(String message) {
        super(message);
    }
}
