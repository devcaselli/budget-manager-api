package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PatchPayerBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerPatchInput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalPatchPayerBoundary implements PatchPayerBoundary {

    private final PatchPayerBoundary delegate;

    public TransactionalPatchPayerBoundary(PatchPayerBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public PayerOutput execute(String id, PayerPatchInput patch, String ownerId) {
        return delegate.execute(id, patch, ownerId);
    }
}
