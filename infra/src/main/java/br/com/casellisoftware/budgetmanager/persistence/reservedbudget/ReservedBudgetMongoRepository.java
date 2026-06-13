package br.com.casellisoftware.budgetmanager.persistence.reservedbudget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReservedBudgetMongoRepository extends MongoRepository<ReservedBudgetDocument, String> {

    @Query("{ '$and': [ { 'ownerId': ?1 }, { 'startMonth': { '$lte': ?0 } }, { 'deleted': { '$ne': true } } ] }")
    List<ReservedBudgetDocument> findActiveFor(String month, String ownerId);

    Optional<ReservedBudgetDocument> findByIdAndOwnerId(String id, String ownerId);

    @Query("{ '$and': [ { 'ownerId': ?0 }, { 'deleted': { '$ne': true } } ] }")
    Page<ReservedBudgetDocument> findAllByOwnerId(String ownerId, Pageable pageable);
}
