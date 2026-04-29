package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FindAllWalletsUseCase implements FindAllWalletsBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindAllWalletsUseCase.class);

    private final WalletRepository walletRepository;

    public FindAllWalletsUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public List<WalletOutput> execute() {
        log.debug("Finding all wallets");
        return walletRepository.findAll()
                .stream()
                .map(WalletOutputAssembler::from)
                .toList();
    }
}
