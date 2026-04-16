package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveBulletUseCase implements SaveBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final FindWalletByIdBoundary findWalletByIdBoundary;

    public SaveBulletUseCase(BulletRepository bulletRepository,
                             FindWalletByIdBoundary findWalletByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.findWalletByIdBoundary = findWalletByIdBoundary;
    }

    @Override
    public BulletOutput execute(BulletInput input) {
        log.info("Saving bullet for walletId={}", input.walletId());

        String walletId = findWalletByIdBoundary.findById(input.walletId()).id();

        Money budget = Money.of(input.budget());
        Bullet bullet = Bullet.create(input.description(), budget, budget, walletId);

        Bullet saved = bulletRepository.save(bullet);
        log.info("Bullet saved successfully, id={}", saved.getId());

        return BulletOutputAssembler.from(saved);
    }
}
