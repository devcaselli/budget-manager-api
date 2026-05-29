package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveWalletBoundary implements SaveWalletBoundary {

    private final SaveWalletBoundary delegate;

    public TransactionalSaveWalletBoundary(SaveWalletBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public WalletOutput execute(WalletInput input) {
        return delegate.execute(input);
    }
}
