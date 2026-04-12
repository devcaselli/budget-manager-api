package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

public interface FindExpensesByWalletIdBoundary {

    PageResult<ExpenseOutput> execute(String walletId, int page, int size);
}
