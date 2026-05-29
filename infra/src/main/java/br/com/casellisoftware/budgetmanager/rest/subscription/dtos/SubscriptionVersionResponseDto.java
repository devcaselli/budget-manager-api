package br.com.casellisoftware.budgetmanager.rest.subscription.dtos;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SubscriptionVersionResponseDto(
        YearMonth effectiveMonth,
        BigDecimal amount
) {
}
