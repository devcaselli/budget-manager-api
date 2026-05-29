package br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * HTTP response DTO for extra-budget endpoints.
 */
public record ExtraBudgetResponseDto(
        String id,
        String description,
        String walletId,
        BigDecimal amount,
        String currency,
        List<AllocationResponseDto> allocations,
        boolean deleted,
        LocalDateTime deletedAt
) {
}
