package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.flag.FlagAwareExecutor;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SaveBulletUseCase implements SaveBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveBulletUseCase.class);

    private final FlagAwareExecutor<BulletInput, BulletOutput> executor;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public SaveBulletUseCase(FlagAwareExecutor<BulletInput, BulletOutput> executor,
                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary,
                "findWalletDomainByIdBoundary must not be null");
    }

    @Override
    public BulletOutput execute(BulletInput input) {
        log.info("Saving bullet for walletId={}", input.walletId());
        Wallet wallet = findWalletDomainByIdBoundary.findById(input.walletId(), input.ownerId());
        return executor.execute(wallet.getFlag(), input);
    }
}
