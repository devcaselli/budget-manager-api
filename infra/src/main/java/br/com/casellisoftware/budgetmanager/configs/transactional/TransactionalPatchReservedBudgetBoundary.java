package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalPatchReservedBudgetBoundary implements PatchReservedBudgetBoundary {

    private final PatchReservedBudgetBoundary delegate;

    public TransactionalPatchReservedBudgetBoundary(PatchReservedBudgetBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ReservedBudgetOutput execute(PatchReservedBudgetInput input) {
        return delegate.execute(input);
    }
}
