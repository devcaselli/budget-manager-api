package br.com.casellisoftware.budgetmanager.persistence.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExpenseMongoRepository extends MongoRepository<ExpenseDocument, String> {

    Page<ExpenseDocument> findByWalletId(String walletId, Pageable pageable);
}
