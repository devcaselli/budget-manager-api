package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;

import java.util.List;

public class FindAllBulletsByIdsUseCase implements FindAllBulletsByIdsBoundary {

    private final BulletRepository bulletRepository;

    public FindAllBulletsByIdsUseCase(BulletRepository bulletRepository) {
        this.bulletRepository = bulletRepository;
    }


    public List<BulletOutput> execute(List<String> ids){
        return this.bulletRepository.findAllByIds(ids)
                .stream()
                .map(BulletOutputAssembler::from)
                .toList();
    }
}
