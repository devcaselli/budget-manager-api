package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ReservedBudgetResponseDto(
        String id,
        String description,
        String details,
        String currency,
        YearMonth startMonth,
        List<ReservedBudgetVersionResponseDto> versions,
        List<ReservedBudgetLinkResponseDto> links,
        boolean deleted,
        FlagEnum flag,
        BigDecimal consumedAmount,
        BigDecimal remainingAmount
) {
}
