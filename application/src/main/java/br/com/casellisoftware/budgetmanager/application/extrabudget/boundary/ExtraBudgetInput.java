package br.com.casellisoftware.budgetmanager.application.extrabudget.boundary;

import java.math.BigDecimal;
import java.util.List;

/**
 * Input DTO for the save-extra-budget use case. Carries raw primitive types from the
 * interface adapter layer; canonical validation happens when the use case calls
 * {@code ExtraBudget.create(...)}.
 */
public record ExtraBudgetInput(
        String description,
        String walletId,
        BigDecimal amount,
        List<AllocationInput> allocations,
        String ownerId
) {
    public ExtraBudgetInput withOwnerId(String ownerId) {
        return new ExtraBudgetInput(description, walletId, amount, allocations, ownerId);
    }
}
