package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

import java.util.List;

public interface FindExtraBudgetsByBulletIdBoundary {

    List<ExtraBudgetOutput> execute(String bulletId, String ownerId);
}
