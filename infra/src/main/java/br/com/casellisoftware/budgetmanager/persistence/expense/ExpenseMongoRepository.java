package br.com.casellisoftware.budgetmanager.persistence.expense;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ExpenseMongoRepository extends MongoRepository<ExpenseDocument, String> {


    List<ExpenseDocument> findAllByWalletId(String walletId);
}
