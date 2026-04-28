package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalDeleteBulletByIdBoundary implements DeleteBulletByIdBoundary {

    private final DeleteBulletByIdBoundary delegate;

    public TransactionalDeleteBulletByIdBoundary(DeleteBulletByIdBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String id) {
        delegate.execute(id);
    }
}
