package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;

public class FindWalletDomainByIdUseCase implements FindWalletDomainByIdBoundary {

    private final WalletRepository walletRepository;

    public FindWalletDomainByIdUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public Wallet findById(String id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }
}
