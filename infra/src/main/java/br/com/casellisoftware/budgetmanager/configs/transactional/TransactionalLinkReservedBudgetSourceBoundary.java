package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.LinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.LinkReservedBudgetSourceInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalLinkReservedBudgetSourceBoundary implements LinkReservedBudgetSourceBoundary {

    private final LinkReservedBudgetSourceBoundary delegate;

    public TransactionalLinkReservedBudgetSourceBoundary(LinkReservedBudgetSourceBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ReservedBudgetOutput execute(LinkReservedBudgetSourceInput input) {
        return delegate.execute(input);
    }
}
