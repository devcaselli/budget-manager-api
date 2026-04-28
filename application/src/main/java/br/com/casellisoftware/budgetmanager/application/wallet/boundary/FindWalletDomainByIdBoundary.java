package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;

public interface FindWalletDomainByIdBoundary {

    Wallet findById(String id);
}
