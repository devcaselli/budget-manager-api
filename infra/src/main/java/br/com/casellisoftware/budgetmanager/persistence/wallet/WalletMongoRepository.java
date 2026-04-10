package br.com.casellisoftware.budgetmanager.persistence.wallet;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface WalletMongoRepository extends MongoRepository<WalletDocument, String> {
}
