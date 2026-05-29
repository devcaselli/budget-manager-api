package br.com.casellisoftware.budgetmanager.domain.extrabudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

/**
 * Value object representing a single allocation within an {@link ExtraBudget}.
 *
 * <p>Immutable record; {@code amount} must be positive.</p>
 */
public record ExtraBudgetAllocation(String bulletId, Money amount) { }
