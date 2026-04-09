package br.com.casellisoftware.budgetmanager.persistence.expense;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ExpenseMongoRepository extends MongoRepository<ExpenseDocument, String> {
}
