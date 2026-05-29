package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

import java.math.BigDecimal;

/**
 * Output DTO for a single allocation within an {@link ExtraBudgetOutput}.
 */
public record AllocationOutput(String bulletId, BigDecimal amount) { }
