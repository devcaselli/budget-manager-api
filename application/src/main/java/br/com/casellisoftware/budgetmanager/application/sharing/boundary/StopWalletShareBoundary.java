package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

public interface StopWalletShareBoundary {

    /**
     * Stops a recurring share from the given wallet's effective month onward,
     * preserving past/closed wallets. Non-destructive: no payment reversal.
     */
    void execute(String walletId, String shareId, String ownerId);
}
