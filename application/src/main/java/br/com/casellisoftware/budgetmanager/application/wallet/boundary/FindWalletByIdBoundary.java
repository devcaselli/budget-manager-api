package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

public interface FindWalletByIdBoundary {

    /**
     * @throws br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException if no wallet exists with the given id
     */
    WalletOutput findById(String id);
}
