package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.policy.BulletAllocationPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SaveBulletUseCase implements SaveBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveBulletUseCase.class);

    private final BulletRepository bulletRepository;
    private final WalletRepository walletRepository;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    public SaveBulletUseCase(BulletRepository bulletRepository,
                             WalletRepository walletRepository,
                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.walletRepository = walletRepository;
        this.findWalletDomainByIdBoundary = findWalletDomainByIdBoundary;
    }

    @Override
    public BulletOutput execute(BulletInput input) {
        log.info("Saving bullet for walletId={}", input.walletId());

        Wallet wallet = findWalletDomainByIdBoundary.findById(input.walletId());
        Money budget = Money.of(input.budget());

        BulletAllocationPolicy.validateAllocation(wallet, budget);

        Wallet debited = wallet.debit(budget);
        Bullet bullet = Bullet.create(input.description(), budget, budget, wallet.getId());

        walletRepository.save(debited);
        Bullet saved = bulletRepository.save(bullet);
        log.info("Bullet saved id={} walletRemaining={}", saved.getId(), debited.getRemaining().amount());

        return BulletOutputAssembler.from(saved);
    }
}
