package br.com.casellisoftware.budgetmanager.persistence.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMongoRepository extends MongoRepository<PaymentDocument, String> {

    Page<PaymentDocument> findByWalletId(String walletId, Pageable pageable);
    Page<PaymentDocument> findByWalletIdAndOwnerId(String walletId, String ownerId, Pageable pageable);
    Optional<PaymentDocument> findByIdAndOwnerId(String id, String ownerId);
    long deleteByIdAndOwnerId(String id, String ownerId);
    List<PaymentDocument> findAllByExpenseId(String expenseId);
    List<PaymentDocument> findAllByExpenseIdAndOwnerId(String expenseId, String ownerId);
    boolean existsByBulletId(String bulletId);
    boolean existsByBulletIdAndOwnerId(String bulletId, String ownerId);
}
