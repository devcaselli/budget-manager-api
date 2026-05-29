package br.com.casellisoftware.budgetmanager.persistence.auth;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RevokedTokenMongoRepository extends MongoRepository<RevokedTokenDocument, String> {
    boolean existsById(String jti);
}
