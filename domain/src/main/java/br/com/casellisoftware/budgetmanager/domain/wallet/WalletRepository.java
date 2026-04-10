package br.com.casellisoftware.budgetmanager.domain.wallet;

import java.util.Optional;

public interface WalletRepository {

    Optional<Wallet> findById(String id);
    Wallet save(Wallet wallet);
}
