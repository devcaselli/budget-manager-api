package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import java.util.Optional;

public interface FindWalletByIdBoundary {

    Optional<WalletOutput> findById(String id);
}