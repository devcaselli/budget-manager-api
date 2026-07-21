package br.com.casellisoftware.budgetmanager.persistence.pluggy;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PluggyConnectionMongoRepository extends MongoRepository<PluggyConnectionDocument, String> {

    List<PluggyConnectionDocument> findByOwnerId(String ownerId);

    Optional<PluggyConnectionDocument> findByItemIdAndOwnerId(String itemId, String ownerId);

    Optional<PluggyConnectionDocument> findByItemId(String itemId);
}
