package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

import java.util.List;

public interface FindExtraBudgetsByWalletIdBoundary {

    List<ExtraBudgetOutput> execute(String walletId, String ownerId);
}
