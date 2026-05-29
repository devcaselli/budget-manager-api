package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveBulletBoundary implements SaveBulletBoundary {

    private final SaveBulletBoundary delegate;

    public TransactionalSaveBulletBoundary(SaveBulletBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public BulletOutput execute(BulletInput input) {
        return delegate.execute(input);
    }
}
