package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.shared.PatchHelper;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchBulletUseCase implements PatchBulletBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchBulletUseCase.class);

    private final BulletRepository bulletRepository;

    public PatchBulletUseCase(BulletRepository bulletRepository) {
        this.bulletRepository = bulletRepository;
    }

    @Override
    public BulletOutput execute(PatchBulletInput input) {
        log.info("Patching bullet id={}", input.id());

        Bullet existing = bulletRepository.findById(input.id())
                .orElseThrow(() -> new BulletNotFoundException(input.id()));

        Bullet patched = PatchHelper.applyPatch(existing, input);

        Bullet saved = bulletRepository.save(patched);
        log.info("Bullet patched successfully, id={}", saved.getId());

        return BulletOutputAssembler.from(saved);
    }
}
