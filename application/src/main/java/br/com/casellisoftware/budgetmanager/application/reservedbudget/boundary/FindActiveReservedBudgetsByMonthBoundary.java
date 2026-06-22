package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import java.time.YearMonth;
import java.util.List;

public interface FindActiveReservedBudgetsByMonthBoundary {

    List<ReservedBudgetOutput> execute(YearMonth month, String ownerId);
}
