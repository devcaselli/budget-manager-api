package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.SaveShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveShareBoundary implements SaveShareBoundary {

    private final SaveShareBoundary delegate;

    public TransactionalSaveShareBoundary(SaveShareBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ShareOutput execute(ShareInput input) {
        return delegate.execute(input);
    }
}
