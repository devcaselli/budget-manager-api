package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository {

    Subscription save(Subscription subscription);

    Optional<Subscription> findById(String id);
    default Optional<Subscription> findById(String id, String ownerId) {
        return findById(id).filter(subscription -> subscription.getOwnerId().equals(ownerId));
    }

    List<Subscription> findActiveFor(YearMonth month);
    default List<Subscription> findActiveForByOwnerId(YearMonth month, String ownerId) {
        return findActiveFor(month).stream()
                .filter(subscription -> subscription.getOwnerId().equals(ownerId))
                .toList();
    }

    List<Subscription> findActiveFor(YearMonth month, SubscriptionState state);
    default List<Subscription> findActiveFor(YearMonth month, SubscriptionState state, String ownerId) {
        return findActiveFor(month, state).stream()
                .filter(subscription -> subscription.getOwnerId().equals(ownerId))
                .toList();
    }

    PageResult<Subscription> findAll(int page, int size);
    PageResult<Subscription> findAll(int page, int size, String ownerId);

    void deleteById(String id);
    default void deleteById(String id, String ownerId) {
        findById(id, ownerId).ifPresent(subscription -> deleteById(id));
    }

    /**
     * Returns {@code true} if there is at least one non-ended subscription
     * tied to {@code creditCardId} owned by {@code ownerId}. Used by
     * {@code DeleteCreditCardByIdUseCase} to block deletion of a credit card
     * still referenced by an active subscription.
     */
    boolean existsActiveByCreditCardId(String creditCardId, String ownerId);
}
