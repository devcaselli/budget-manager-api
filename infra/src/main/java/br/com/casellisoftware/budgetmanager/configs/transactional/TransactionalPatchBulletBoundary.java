package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalPatchBulletBoundary implements PatchBulletBoundary {

    private final PatchBulletBoundary delegate;

    public TransactionalPatchBulletBoundary(PatchBulletBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public BulletOutput execute(PatchBulletInput input) {
        return delegate.execute(input);
    }
}
