package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;

import java.time.YearMonth;

public record ReservedBudgetLinkOutput(
        ReservedBudgetLinkSourceType sourceType,
        String sourceId,
        YearMonth fromMonth
) {
}
