package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

import java.math.BigDecimal;

/**
 * Input DTO for a single allocation within an {@link ExtraBudgetInput}.
 */
public record AllocationInput(String bulletId, BigDecimal amount) { }
