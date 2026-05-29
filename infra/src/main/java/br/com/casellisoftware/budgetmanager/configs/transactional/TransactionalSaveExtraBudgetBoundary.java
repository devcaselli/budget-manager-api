package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.SaveExtraBudgetBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveExtraBudgetBoundary implements SaveExtraBudgetBoundary {

    private final SaveExtraBudgetBoundary delegate;

    public TransactionalSaveExtraBudgetBoundary(SaveExtraBudgetBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ExtraBudgetOutput execute(ExtraBudgetInput input) {
        return delegate.execute(input);
    }
}
