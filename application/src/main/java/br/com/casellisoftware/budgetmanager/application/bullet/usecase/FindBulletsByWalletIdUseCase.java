package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;

import java.util.List;

public class FindBulletsByWalletIdUseCase implements FindBulletsByWalletIdBoundary {

    private final BulletRepository bulletRepository;
    private final FindWalletByIdBoundary findWalletByIdBoundary;

    public FindBulletsByWalletIdUseCase(BulletRepository bulletRepository,
                                        FindWalletByIdBoundary findWalletByIdBoundary) {
        this.bulletRepository = bulletRepository;
        this.findWalletByIdBoundary = findWalletByIdBoundary;
    }

    @Override
    public List<BulletOutput> execute(String walletId, String ownerId) {
        findWalletByIdBoundary.findById(walletId, ownerId);

        return bulletRepository.findByWalletId(walletId, ownerId)
                .stream()
                .map(BulletOutputAssembler::from)
                .toList();
    }
}
