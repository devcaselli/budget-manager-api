package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FindWalletByIdUseCase implements FindWalletByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindWalletByIdUseCase.class);

    private final WalletRepository walletRepository;

    public FindWalletByIdUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletOutput findById(String id) {
        log.debug("Finding wallet by id {}", id);
        Wallet wallet = walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));

        log.debug("Wallet found {}", wallet);
        return WalletOutputAssembler.from(wallet);
    }
}
