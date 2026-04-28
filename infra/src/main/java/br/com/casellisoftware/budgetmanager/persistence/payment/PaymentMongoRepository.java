package br.com.casellisoftware.budgetmanager.persistence.payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PaymentMongoRepository extends MongoRepository<PaymentDocument, String> {

    Page<PaymentDocument> findByWalletId(String walletId, Pageable pageable);
    List<PaymentDocument> findAllByExpenseId(String expenseId);
    boolean existsByBulletId(String bulletId);
}
