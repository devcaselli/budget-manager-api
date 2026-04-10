package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SaveWalletUseCase implements SaveWalletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveWalletUseCase.class);


    private final WalletRepository walletRepository;

    public SaveWalletUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletOutput execute(WalletInput input) {

        Wallet wallet = Wallet.create(
                input.description(),
                Money.of(input.budget()),
                input.closedDate(),
                input.startDate(),
                input.isClosed()
        );

        WalletOutput saved = WalletOutputAssembler.from(
                this.walletRepository.save(wallet)
        );

        log.info("Wallet saved successfully, id={}", saved.id());
        log.info("Wallet starts on {}", saved.startDate());

        Optional.ofNullable(saved.closedDate())
                .ifPresent(data -> {
                    log.info("Wallet closes on {}", data);
                });

        return saved;
    }
}
