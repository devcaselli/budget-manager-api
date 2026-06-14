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

    /**
     * Cardinality check for the Vínculos feature: finds the reserved budget (if any)
     * whose {@code links} array contains an entry matching the given source.
     *
     * <p>Uses {@code $elemMatch} for correct multikey-index semantics — without it a
     * compound index query could match across different array elements.</p>
     *
     * @param sourceType string name of {@link br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType}
     * @param sourceId   the linked item's ID
     * @param ownerId    owner filter (isolates tenants)
     */
    @Query("{ 'ownerId': ?2, 'deleted': { '$ne': true }, 'links': { '$elemMatch': { 'sourceType': ?0, 'sourceId': ?1 } } }")
    Optional<ReservedBudgetDocument> findByLinkedSource(String sourceType, String sourceId, String ownerId);
}
