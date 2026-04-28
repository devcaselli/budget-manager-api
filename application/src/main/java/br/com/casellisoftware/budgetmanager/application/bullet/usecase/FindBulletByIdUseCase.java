package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindBulletByIdUseCase implements FindBulletByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindBulletByIdUseCase.class);

    private final BulletRepository bulletRepository;

    public FindBulletByIdUseCase(BulletRepository bulletRepository) {
        this.bulletRepository = bulletRepository;
    }

    @Override
    public BulletOutput execute(String id) {
        log.info("Finding bullet by id={}", id);

        Bullet bullet = bulletRepository.findById(id)
                .orElseThrow(() -> new BulletNotFoundException(id));

        log.info("Bullet found, id={}", bullet.getId());
        return BulletOutputAssembler.from(bullet);
    }
}
