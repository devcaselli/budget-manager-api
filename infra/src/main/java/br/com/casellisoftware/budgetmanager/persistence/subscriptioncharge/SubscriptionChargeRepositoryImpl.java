package br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge;
import br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionChargeRepository;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.mappers.SubscriptionChargePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SubscriptionChargeRepositoryImpl implements SubscriptionChargeRepository {

    private final SubscriptionChargeMongoRepository subscriptionChargeMongoRepository;
    private final SubscriptionChargePersistenceMapper mapper;

    @Override
    public SubscriptionCharge save(SubscriptionCharge subscriptionCharge) {
        Long version = subscriptionChargeMongoRepository.findById(subscriptionCharge.getId())
                .map(SubscriptionChargeDocument::getVersion)
                .orElse(null);
        SubscriptionChargeDocument saved = subscriptionChargeMongoRepository.save(mapper.toDocument(subscriptionCharge, version));
        return mapper.toDomain(saved);
    }

    @Override
    public List<SubscriptionCharge> findByWalletId(String walletId) {
        return subscriptionChargeMongoRepository.findByWalletId(walletId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<SubscriptionCharge> findByWalletId(String walletId, String ownerId) {
        return subscriptionChargeMongoRepository.findByWalletIdAndOwnerId(walletId, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<SubscriptionCharge> findById(String id) {
        return subscriptionChargeMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<SubscriptionCharge> findById(String id, String ownerId) {
        return subscriptionChargeMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByWalletIdAndSubscriptionIdAndMonth(String walletId, String subscriptionId, YearMonth month) {
        return subscriptionChargeMongoRepository.existsByWalletIdAndSubscriptionIdAndMonth(walletId, subscriptionId, month);
    }

    @Override
    public boolean existsByWalletIdAndSubscriptionIdAndMonth(String walletId, String subscriptionId, YearMonth month, String ownerId) {
        return subscriptionChargeMongoRepository.existsByWalletIdAndSubscriptionIdAndMonthAndOwnerId(walletId, subscriptionId, month, ownerId);
    }
}
