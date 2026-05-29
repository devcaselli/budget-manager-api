package br.com.casellisoftware.budgetmanager.persistence.subscription;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubscriptionMongoRepository extends MongoRepository<SubscriptionDocument, String> {

    @Query("{ '$and': [ { 'startMonth': { '$lte': ?0 } }, { '$or': [ { 'endMonth': null }, { 'endMonth': { '$gt': ?0 } } ] }, { '$or': [ { 'state': { '$in': ?1 } }, { 'state': null } ] } ] }")
    List<SubscriptionDocument> findActiveForAnyState(String month, List<String> states);

    @Query("{ '$and': [ { 'ownerId': ?2 }, { 'startMonth': { '$lte': ?0 } }, { '$or': [ { 'endMonth': null }, { 'endMonth': { '$gt': ?0 } } ] }, { '$or': [ { 'state': { '$in': ?1 } }, { 'state': null } ] } ] }")
    List<SubscriptionDocument> findActiveForAnyState(String month, List<String> states, String ownerId);

    @Query("{ '$and': [ { 'startMonth': { '$lte': ?0 } }, { '$or': [ { 'endMonth': null }, { 'endMonth': { '$gt': ?0 } } ] }, { '$or': [ { 'state': 'PRODUCTION' }, { 'state': null } ] } ] }")
    List<SubscriptionDocument> findActiveFor(String month);

    @Query("{ '$and': [ { 'ownerId': ?1 }, { 'startMonth': { '$lte': ?0 } }, { '$or': [ { 'endMonth': null }, { 'endMonth': { '$gt': ?0 } } ] }, { '$or': [ { 'state': 'PRODUCTION' }, { 'state': null } ] } ] }")
    List<SubscriptionDocument> findActiveFor(String month, String ownerId);

    Optional<SubscriptionDocument> findByIdAndOwnerId(String id, String ownerId);

    org.springframework.data.domain.Page<SubscriptionDocument> findAllByOwnerId(String ownerId, org.springframework.data.domain.Pageable pageable);

    @Query(value = "{ 'ownerId': ?1, 'creditCardId': ?0, '$or': [ { 'endMonth': null }, { 'endMonth': { '$gt': ?2 } } ] }", exists = true)
    boolean existsActiveByCreditCardIdAndOwnerId(String creditCardId, String ownerId, String currentMonth);
}
