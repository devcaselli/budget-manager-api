package br.com.casellisoftware.budgetmanager.persistence.payer;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayerMongoRepository extends MongoRepository<PayerDocument, String> {

    Optional<PayerDocument> findByIdAndOwnerId(String id, String ownerId);

    List<PayerDocument> findAllByOwnerIdAndDeletedFalse(String ownerId);

    List<PayerDocument> findAllByOwnerIdAndTypeAndDeletedFalse(String ownerId, String type);

    List<PayerDocument> findAllByOwnerIdAndWalletIdAndDeletedFalse(String ownerId, String walletId);

    List<PayerDocument> findAllByIdInAndOwnerIdAndDeletedFalse(Collection<String> ids, String ownerId);

    List<PayerDocument> findAllByWalletIdAndOwnerIdAndDeletedFalse(String walletId, String ownerId);
}
