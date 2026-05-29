package br.com.casellisoftware.budgetmanager.persistence.wallet;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WalletMongoRepository extends MongoRepository<WalletDocument, String> {

    /**
     * effectiveMonth must be the serialized YearMonth string ({@code YYYY-MM}); passing
     * a {@link java.time.YearMonth} directly causes Spring Data to BSON-encode it as a
     * sub-document (year/month) instead of running the registered
     * {@code YearMonthWriteConverter}, which silently breaks the equality match.
     */
    @Query("{ 'effectiveMonth': ?0, 'state': 'PRODUCTION', 'isClosed': { $ne: true }, "
            + "$or: [ { 'closedDate': null }, { 'closedDate': { $gt: ?1 } } ] }")
    Optional<WalletDocument> findCurrentProductionOpen(String effectiveMonth, LocalDate today);

    Optional<WalletDocument> findByIdAndOwnerId(String id, String ownerId);

    List<WalletDocument> findAllByOwnerId(String ownerId);

    @Query("{ 'ownerId': ?2, 'effectiveMonth': ?0, 'state': 'PRODUCTION', 'isClosed': { $ne: true }, "
            + "$or: [ { 'closedDate': null }, { 'closedDate': { $gt: ?1 } } ] }")
    Optional<WalletDocument> findCurrentProductionOpen(String effectiveMonth, LocalDate today, String ownerId);
}
