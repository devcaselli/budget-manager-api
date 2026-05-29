package br.com.casellisoftware.budgetmanager.domain.subscriptioncharge;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SubscriptionChargeRepository {

    SubscriptionCharge save(SubscriptionCharge subscriptionCharge);

    List<SubscriptionCharge> findByWalletId(String walletId);
    default List<SubscriptionCharge> findByWalletId(String walletId, String ownerId) {
        return findByWalletId(walletId).stream()
                .filter(subscriptionCharge -> subscriptionCharge.getOwnerId().equals(ownerId))
                .toList();
    }

    Optional<SubscriptionCharge> findById(String id);
    default Optional<SubscriptionCharge> findById(String id, String ownerId) {
        return findById(id).filter(subscriptionCharge -> subscriptionCharge.getOwnerId().equals(ownerId));
    }

    /**
     * Idempotency check used by backfill/reconciler pipelines: returns whether a
     * charge already exists for the given (walletId, subscriptionId, month)
     * triple. Backed by a unique index on the persistence layer.
     */
    boolean existsByWalletIdAndSubscriptionIdAndMonth(String walletId, String subscriptionId, YearMonth month);
    default boolean existsByWalletIdAndSubscriptionIdAndMonth(String walletId, String subscriptionId, YearMonth month, String ownerId) {
        return existsByWalletIdAndSubscriptionIdAndMonth(walletId, subscriptionId, month);
    }
}
