package br.com.casellisoftware.budgetmanager.persistence.creditcard;

import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface CreditCardMongoRepository extends MongoRepository<CreditCardDocument, String> {

    Optional<CreditCardDocument> findByIdAndOwnerId(String id, String ownerId);

    Page<CreditCardDocument> findAllByOwnerId(String ownerId, Pageable pageable);

    /** Matches documents where {@code normalizedLabels} array contains the given value. */
    Optional<CreditCardDocument> findByNormalizedLabelsContainingAndOwnerId(String normalizedLabel, String ownerId);

    Optional<CreditCardDocument> findByNameAndOwnerId(String name, String ownerId);
}
