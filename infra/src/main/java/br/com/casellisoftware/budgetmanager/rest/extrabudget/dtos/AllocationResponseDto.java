package br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos;

import java.math.BigDecimal;

/**
 * HTTP response DTO for a single allocation within an {@link ExtraBudgetResponseDto}.
 */
public record AllocationResponseDto(String bulletId, BigDecimal amount) {
}
