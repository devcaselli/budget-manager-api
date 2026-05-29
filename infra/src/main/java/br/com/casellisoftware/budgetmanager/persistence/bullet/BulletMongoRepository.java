package br.com.casellisoftware.budgetmanager.persistence.bullet;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BulletMongoRepository extends MongoRepository<BulletDocument, String> {

    List<BulletDocument> findByWalletId(String walletId);

    Optional<BulletDocument> findByIdAndOwnerId(String id, String ownerId);

    List<BulletDocument> findAllByIdInAndOwnerId(List<String> ids, String ownerId);

    List<BulletDocument> findByWalletIdAndOwnerId(String walletId, String ownerId);
}
