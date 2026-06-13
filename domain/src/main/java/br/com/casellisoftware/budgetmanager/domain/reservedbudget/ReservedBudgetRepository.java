package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * Port for {@link ReservedBudget} persistence. "Not found" is expressed as an empty
 * {@link Optional}, never an exception. Deletion is logical (via {@code save} of a
 * {@link ReservedBudget#markDeleted(java.time.LocalDateTime) deleted} instance), so there is
 * no physical {@code deleteById}.
 */
public interface ReservedBudgetRepository {

    ReservedBudget save(ReservedBudget reservedBudget);

    Optional<ReservedBudget> findById(String id);

    default Optional<ReservedBudget> findById(String id, String ownerId) {
        return findById(id).filter(reservedBudget -> reservedBudget.getOwnerId().equals(ownerId));
    }

    /**
     * Non-deleted reserved budgets active for {@code month} (i.e. {@code startMonth <= month}),
     * scoped to {@code ownerId}. These are the ones deducted from a wallet of that month.
     */
    List<ReservedBudget> findActiveFor(YearMonth month, String ownerId);

    /**
     * Batch variant: non-deleted reserved budgets active for <em>any</em> of {@code months}
     * (i.e. {@code startMonth <= max(months)}), scoped to {@code ownerId}. Callers filter
     * per-month with {@link ReservedBudget#isApplicable(YearMonth)}. Keeps the wallet-list
     * deduction query at O(owners) DB calls.
     */
    List<ReservedBudget> findActiveForAny(List<YearMonth> months, String ownerId);

    PageResult<ReservedBudget> findAll(int page, int size, String ownerId);
}
