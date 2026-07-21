package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public record ReservedBudgetOutput(
        String id,
        String description,
        String details,
        String currency,
        YearMonth startMonth,
        List<ReservedBudgetVersionOutput> versions,
        List<ReservedBudgetLinkOutput> links,
        boolean deleted,
        FlagEnum flag,
        BigDecimal consumedAmount,
        BigDecimal remainingAmount
) {
}
