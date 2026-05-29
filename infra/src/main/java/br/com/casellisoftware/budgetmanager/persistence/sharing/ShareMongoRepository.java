package br.com.casellisoftware.budgetmanager.persistence.sharing;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ShareMongoRepository extends MongoRepository<ShareDocument, String> {

    Optional<ShareDocument> findByIdAndOwnerId(String id, String ownerId);

    Optional<ShareDocument> findBySourceTypeAndSourceIdAndStatusAndOwnerId(
            String sourceType, String sourceId, String status, String ownerId);

    List<ShareDocument> findAllBySourceTypeAndSourceIdInAndStatusAndOwnerId(
            String sourceType, Collection<String> sourceIds, String status, String ownerId);

    List<ShareDocument> findAllByOwnerId(String ownerId);

    boolean existsBySourceTypeAndSourceIdAndStatusAndOwnerId(
            String sourceType, String sourceId, String status, String ownerId);

    boolean existsByOwnerIdAndQuotasPayerId(String ownerId, String payerId);

    List<ShareDocument> findAllByOwnerIdAndStatusAndQuotasPayerId(String ownerId,
                                                                  String status,
                                                                  String payerId);
}
