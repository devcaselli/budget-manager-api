package br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SubscriptionChargeMongoRepository extends MongoRepository<SubscriptionChargeDocument, String> {

    List<SubscriptionChargeDocument> findByWalletId(String walletId);

    List<SubscriptionChargeDocument> findByWalletIdAndOwnerId(String walletId, String ownerId);

    Optional<SubscriptionChargeDocument> findByIdAndOwnerId(String id, String ownerId);

    boolean existsByWalletIdAndSubscriptionIdAndMonth(String walletId, String subscriptionId, YearMonth month);

    boolean existsByWalletIdAndSubscriptionIdAndMonthAndOwnerId(String walletId, String subscriptionId, YearMonth month, String ownerId);
}
