package br.com.casellisoftware.budgetmanager.persistence.extrabudget;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ExtraBudgetMongoRepository extends MongoRepository<ExtraBudgetDocument, String> {

    @Query("{ 'walletId': ?0, 'ownerId': ?1, 'deleted': false }")
    List<ExtraBudgetDocument> findActiveByWalletId(String walletId, String ownerId);

    @Query("{ 'allocations.bulletId': ?0, 'ownerId': ?1, 'deleted': false }")
    List<ExtraBudgetDocument> findActiveByBulletId(String bulletId, String ownerId);
}
