package br.com.casellisoftware.budgetmanager.persistence.sync;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface SyncPreferenceMongoRepository extends MongoRepository<SyncPreferenceDocument, String> {

    @Query(value = "{ 'enabled': true }", fields = "{ '_id': 1 }")
    List<SyncPreferenceDocument> findAllByEnabledTrue();
}
