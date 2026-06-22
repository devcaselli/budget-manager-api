package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;

import java.time.YearMonth;

public record ReservedBudgetLinkResponseDto(
        ReservedBudgetLinkSourceType sourceType,
        String sourceId,
        YearMonth fromMonth
) {
}
