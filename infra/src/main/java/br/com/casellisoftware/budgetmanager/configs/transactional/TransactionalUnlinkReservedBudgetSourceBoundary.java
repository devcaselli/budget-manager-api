package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.UnlinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.UnlinkReservedBudgetSourceInput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalUnlinkReservedBudgetSourceBoundary implements UnlinkReservedBudgetSourceBoundary {

    private final UnlinkReservedBudgetSourceBoundary delegate;

    public TransactionalUnlinkReservedBudgetSourceBoundary(UnlinkReservedBudgetSourceBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ReservedBudgetOutput execute(UnlinkReservedBudgetSourceInput input) {
        return delegate.execute(input);
    }
}
