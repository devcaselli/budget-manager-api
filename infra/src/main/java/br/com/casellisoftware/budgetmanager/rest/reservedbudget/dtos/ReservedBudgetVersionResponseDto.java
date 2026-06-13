package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ReservedBudgetVersionResponseDto(
        YearMonth effectiveMonth,
        BigDecimal amount
) {
}
