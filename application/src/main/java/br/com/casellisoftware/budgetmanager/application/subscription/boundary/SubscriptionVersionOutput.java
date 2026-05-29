package br.com.casellisoftware.budgetmanager.application.subscription.boundary;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SubscriptionVersionOutput(
        YearMonth effectiveMonth,
        BigDecimal amount
) {
}
