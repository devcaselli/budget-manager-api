package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain port for {@link Expense} persistence.
 *
 * <p>Intentionally minimal: exposes only the primitives required by existing use
 * cases. New operations (e.g., {@code update}, {@code deleteById}) must be added
 * together with the use case that consumes them — not speculatively.</p>
 *
 * <p>Contract rules:
 * <ul>
 *   <li>Adapters must not throw domain exceptions for reads. "Not found" is a
 *       use case decision, signaled by {@link Optional#empty()}.</li>
 *   <li>Scoped delete may throw {@link ExpenseNotFoundException} when no row
 *       matches both id and ownerId, including races after a use-case pre-check.</li>
 *   <li>Adapters must not throw {@code IllegalArgumentException} for invariants
 *       that the domain entity already guarantees.</li>
 * </ul>
 */
public interface ExpenseRepository {

    Expense save(Expense expense);

    Optional<Expense> findById(String id);
    default Optional<Expense> findById(String id, String ownerId) {
        return findById(id).filter(expense -> expense.getOwnerId().equals(ownerId));
    }

    boolean existsById(String id);
    default boolean existsById(String id, String ownerId) {
        return findById(id, ownerId).isPresent();
    }

    boolean existsAnyByCreditCardId(String creditCardId);
    boolean existsAnyByCreditCardId(String creditCardId, String ownerId);

    PageResult<Expense> findByWalletId(String walletId, int page, int size, boolean unhidden);
    /**
     * Owner-scoped variant. Adapters MUST override to filter by ownerId — the
     * default below would leak cross-owner data. It exists only so legacy callers
     * compile during migration. Production paths must rely on the override.
     */
    PageResult<Expense> findByWalletId(String walletId, int page, int size, boolean unhidden, String ownerId);

    List<Expense> findByOwnerIdAndPurchaseDateGreaterThanOrEqual(String ownerId, LocalDate startDate);

    List<Expense> findAllByOwnerId(String ownerId);

    default List<Expense> findByPayerId(String payerId, String ownerId) {
        // TODO payer-expense-link: replace when Expense.payerId is added by the parallel feature.
        return List.of();
    }

    ExpenseByCreditCardResult findByCreditCardId(String creditCardId,
                                                 ExpenseByCreditCardFilter filter,
                                                 int page,
                                                 int size);
    ExpenseByCreditCardResult findByCreditCardId(String creditCardId,
                                                 ExpenseByCreditCardFilter filter,
                                                 int page,
                                                 int size,
                                                 String ownerId);

    Optional<Expense> findByInstallmentId(String installmentId);
    Optional<Expense> findByInstallmentId(String installmentId, String ownerId);

    List<String> findIdsByCreditCardId(String creditCardId);
    List<String> findIdsByCreditCardId(String creditCardId, String ownerId);

    void deleteById(String id, String ownerId);

    /**
     * Finds an expense by its ingest-side pending id, scoped to the owner.
     * Used during sync to skip already-materialized pending items (deduplication).
     */
    Optional<Expense> findBySourcePendingId(String sourcePendingId, String ownerId);
}
