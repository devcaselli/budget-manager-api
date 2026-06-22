package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.SaveReservedBudgetBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveReservedBudgetBoundary implements SaveReservedBudgetBoundary {

    private final SaveReservedBudgetBoundary delegate;

    public TransactionalSaveReservedBudgetBoundary(SaveReservedBudgetBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ReservedBudgetOutput execute(ReservedBudgetInput input) {
        return delegate.execute(input);
    }
}
