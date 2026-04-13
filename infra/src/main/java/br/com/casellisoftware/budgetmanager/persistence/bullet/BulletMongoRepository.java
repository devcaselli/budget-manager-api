package br.com.casellisoftware.budgetmanager.persistence.bullet;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface BulletMongoRepository extends MongoRepository<BulletDocument, String> {
}
