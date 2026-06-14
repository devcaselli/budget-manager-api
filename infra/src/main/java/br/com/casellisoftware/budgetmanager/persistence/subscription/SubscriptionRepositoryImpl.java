package br.com.casellisoftware.budgetmanager.persistence.subscription;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.persistence.subscription.mappers.SubscriptionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class SubscriptionRepositoryImpl implements SubscriptionRepository {

    private final SubscriptionMongoRepository subscriptionMongoRepository;
    private final SubscriptionPersistenceMapper mapper;
    private final Clock clock;

    @Override
    public Subscription save(Subscription subscription) {
        Long version = subscriptionMongoRepository.findById(subscription.getId())
                .map(SubscriptionDocument::getVersion)
                .orElse(null);
        SubscriptionDocument saved = subscriptionMongoRepository.save(mapper.toDocument(subscription, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return subscriptionMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Subscription> findById(String id, String ownerId) {
        return subscriptionMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public List<Subscription> findActiveFor(YearMonth month) {
        String monthAsString = Objects.requireNonNull(month, "month must not be null").toString();
        return subscriptionMongoRepository.findActiveFor(monthAsString)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Subscription> findActiveForByOwnerId(YearMonth month, String ownerId) {
        String monthAsString = Objects.requireNonNull(month, "month must not be null").toString();
        return subscriptionMongoRepository.findActiveFor(monthAsString, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Subscription> findActiveFor(YearMonth month, SubscriptionState state) {
        String monthAsString = Objects.requireNonNull(month, "month must not be null").toString();
        SubscriptionState requiredState = Objects.requireNonNull(state, "state must not be null");
        if (requiredState == SubscriptionState.PREVIEW) {
            return subscriptionMongoRepository.findActiveForAnyState(monthAsString, List.of("PRODUCTION", "PREVIEW"))
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        }
        return subscriptionMongoRepository.findActiveFor(monthAsString)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Subscription> findActiveFor(YearMonth month, SubscriptionState state, String ownerId) {
        String monthAsString = Objects.requireNonNull(month, "month must not be null").toString();
        SubscriptionState requiredState = Objects.requireNonNull(state, "state must not be null");
        if (requiredState == SubscriptionState.PREVIEW) {
            return subscriptionMongoRepository.findActiveForAnyState(monthAsString, List.of("PRODUCTION", "PREVIEW"), ownerId)
                    .stream()
                    .map(mapper::toDomain)
                    .toList();
        }
        return subscriptionMongoRepository.findActiveFor(monthAsString, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Map<String, Subscription> findAllByIds(Collection<String> ids, String ownerId) {
        Objects.requireNonNull(ids, "ids must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        if (ids.isEmpty()) {
            return Map.of();
        }
        return subscriptionMongoRepository.findAllByIdInAndOwnerId(ids, ownerId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(Subscription::getId, s -> s));
    }

    @Override
    public PageResult<Subscription> findAll(int page, int size) {
        Page<SubscriptionDocument> documentPage = subscriptionMongoRepository.findAll(PageRequest.of(page, size));
        List<Subscription> subscriptions = documentPage.getContent()
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResult<>(
                subscriptions,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Override
    public PageResult<Subscription> findAll(int page, int size, String ownerId) {
        Page<SubscriptionDocument> documentPage = subscriptionMongoRepository.findAllByOwnerId(ownerId, PageRequest.of(page, size));
        List<Subscription> subscriptions = documentPage.getContent()
                .stream()
                .map(mapper::toDomain)
                .toList();

        return new PageResult<>(
                subscriptions,
                documentPage.getNumber(),
                documentPage.getSize(),
                documentPage.getTotalElements(),
                documentPage.getTotalPages()
        );
    }

    @Override
    public void deleteById(String id) {
        subscriptionMongoRepository.deleteById(id);
    }

    @Override
    public boolean existsActiveByCreditCardId(String creditCardId, String ownerId) {
        Objects.requireNonNull(creditCardId, "creditCardId must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        String currentMonth = YearMonth.now(clock).toString();
        return subscriptionMongoRepository.existsActiveByCreditCardIdAndOwnerId(creditCardId, ownerId, currentMonth);
    }
}
