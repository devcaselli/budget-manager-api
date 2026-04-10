package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;

import java.util.Optional;

public class FindWalletByIdUseCase {

    private final WalletRepository  walletRepository;

    public FindWalletByIdUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public WalletOutput execute(String id){
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found, id: "+id));

        return WalletOutputAssembler.from(wallet);
    }
}
