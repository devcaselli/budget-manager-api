package br.com.casellisoftware.budgetmanager.domain.expense;

import java.util.Optional;

/**
 * Domain port for {@link Expense} persistence.
 *
 * <p>Intentionally minimal: exposes only the primitives required by existing use
 * cases. New operations (e.g., {@code update}, {@code deleteById},
 * {@code findAllByWalletId}) must be added together with the use case that
 * consumes them — not speculatively.</p>
 *
 * <p>Contract rules:
 * <ul>
 *   <li>Adapters must not throw domain exceptions. "Not found" is a use case
 *       decision, signaled by {@link Optional#empty()}.</li>
 *   <li>Adapters must not throw {@code IllegalArgumentException} for invariants
 *       that the domain entity already guarantees.</li>
 * </ul>
 */
public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(String id);
}
