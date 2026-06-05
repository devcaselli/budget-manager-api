package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import java.util.List;

public interface FindWalletSharesBoundary {

    /**
     * Shares (subscription/installment-sourced) effective for the given wallet's
     * month, including shares created in earlier wallets. Excludes shares already
     * stopped at or before that month. Expense-sourced shares are wallet-local and
     * not returned here.
     */
    List<ShareOutput> execute(String walletId, String ownerId);
}
