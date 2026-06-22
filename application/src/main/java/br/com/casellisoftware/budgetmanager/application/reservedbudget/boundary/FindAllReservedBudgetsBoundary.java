package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

public interface FindAllReservedBudgetsBoundary {

    PageResult<ReservedBudgetOutput> execute(int page, int size, String ownerId);
}
