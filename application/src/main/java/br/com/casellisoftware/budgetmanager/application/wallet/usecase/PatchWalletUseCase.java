package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.shared.PatchHelper;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchWalletUseCase implements PatchWalletBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchWalletUseCase.class);

    private final WalletRepository walletRepository;

    public PatchWalletUseCase(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletOutput execute(PatchWalletInput input) {
        log.info("Patching wallet id={}", input.id());

        Wallet existing = walletRepository.findById(input.id())
                .orElseThrow(() -> new WalletNotFoundException(input.id()));

        Wallet patched = PatchHelper.applyPatch(existing, input);

        Wallet saved = walletRepository.save(patched);
        log.info("Wallet patched successfully, id={}", saved.getId());

        return WalletOutputAssembler.from(saved);
    }
}
