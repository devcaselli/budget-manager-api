package br.com.casellisoftware.budgetmanager.domain.wallet;

import java.util.List;
import java.util.Optional;

public interface WalletRepository {

    Optional<Wallet> findById(String id);
    List<Wallet> findAll();
    Wallet save(Wallet wallet);
}
