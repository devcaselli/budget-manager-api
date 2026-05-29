package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Output DTO for extra-budget use cases.
 */
public record ExtraBudgetOutput(
        String id,
        String ownerId,
        String description,
        String walletId,
        BigDecimal amount,
        String currency,
        List<AllocationOutput> allocations,
        boolean deleted,
        LocalDateTime deletedAt
) { }
