package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerInput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.SavePayerBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSavePayerBoundary implements SavePayerBoundary {

    private final SavePayerBoundary delegate;

    public TransactionalSavePayerBoundary(SavePayerBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public PayerOutput execute(PayerInput input) {
        return delegate.execute(input);
    }
}
