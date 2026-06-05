package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.StopWalletShareBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalStopWalletShareBoundary implements StopWalletShareBoundary {

    private final StopWalletShareBoundary delegate;

    public TransactionalStopWalletShareBoundary(StopWalletShareBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public void execute(String walletId, String shareId, String ownerId) {
        delegate.execute(walletId, shareId, ownerId);
    }
}
