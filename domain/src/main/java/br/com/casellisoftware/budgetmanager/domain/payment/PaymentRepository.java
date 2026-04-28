package br.com.casellisoftware.budgetmanager.domain.payment;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for {@link Payment} persistence.
 *
 * <p>Intentionally minimal: exposes only the primitives required by existing use
 * cases. New operations (e.g., {@code update}, {@code deleteById}) must be added
 * together with the use case that consumes them — not speculatively.</p>
 *
 * <p>Contract rules:
 * <ul>
 *   <li>Adapters must not throw domain exceptions. "Not found" is a use case
 *       decision, signaled by {@link Optional#empty()}.</li>
 *   <li>Adapters must not throw {@code IllegalArgumentException} for invariants
 *       that the domain entity already guarantees.</li>
 * </ul>
 */
public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(String id);

    PageResult<Payment> findByWalletId(String walletId, int page, int size);

    void deleteById(String id);

    List<Payment> findAllByExpenseId(String expenseId);

    void deleteAllById(List<String> ids);

    boolean existsByBulletId(String bulletId);
}
