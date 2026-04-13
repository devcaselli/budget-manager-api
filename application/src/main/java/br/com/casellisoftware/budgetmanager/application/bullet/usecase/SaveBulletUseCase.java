package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletByIdUseCase;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveBulletUseCase implements SaveBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final FindWalletByIdUseCase findWalletByIdUseCase;

    public SaveBulletUseCase(BulletRepository bulletRepository,
                             FindWalletByIdUseCase findWalletByIdUseCase) {
        this.bulletRepository = bulletRepository;
        this.findWalletByIdUseCase = findWalletByIdUseCase;
    }

    @Override
    public BulletOutput execute(BulletInput input) {
        log.info("Saving bullet for walletId={}", input.walletId());

        String walletId = findWalletByIdUseCase.execute(input.walletId()).id();

        Money budget = Money.of(input.budget());
        Bullet bullet = Bullet.create(input.description(), budget, budget, walletId);

        Bullet saved = bulletRepository.save(bullet);
        log.info("Bullet saved successfully, id={}", saved.getId());

        return BulletOutputAssembler.from(saved);
    }
}
