package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ReservedBudgetVersionOutput(
        YearMonth effectiveMonth,
        BigDecimal amount
) {
}
