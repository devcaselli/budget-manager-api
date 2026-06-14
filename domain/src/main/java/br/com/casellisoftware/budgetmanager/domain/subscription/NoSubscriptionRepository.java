package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Null-Object implementation of {@link SubscriptionRepository} for contexts where
 * subscriptions are not relevant (e.g. test harnesses that only care about wallets
 * or bullets). All read methods return empty results; write methods throw
 * {@link UnsupportedOperationException} because this object should never be asked
 * to persist data.
 */
public final class NoSubscriptionRepository implements SubscriptionRepository {

    public static final NoSubscriptionRepository INSTANCE = new NoSubscriptionRepository();

    private NoSubscriptionRepository() {
    }

    @Override
    public Subscription save(Subscription subscription) {
        throw new UnsupportedOperationException("read-only null object");
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return Optional.empty();
    }

    @Override
    public List<Subscription> findActiveFor(YearMonth month) {
        return List.of();
    }

    @Override
    public List<Subscription> findActiveFor(YearMonth month, SubscriptionState state) {
        return List.of();
    }

    @Override
    public PageResult<Subscription> findAll(int page, int size) {
        int effectiveSize = size < 1 ? 1 : size;
        return new PageResult<>(List.of(), page, effectiveSize, 0, 0);
    }

    @Override
    public PageResult<Subscription> findAll(int page, int size, String ownerId) {
        return findAll(page, size);
    }

    @Override
    public void deleteById(String id) {
        throw new UnsupportedOperationException("read-only null object");
    }

    @Override
    public Map<String, Subscription> findAllByIds(Collection<String> ids, String ownerId) {
        return Map.of();
    }

    @Override
    public boolean existsActiveByCreditCardId(String creditCardId, String ownerId) {
        return false;
    }
}
