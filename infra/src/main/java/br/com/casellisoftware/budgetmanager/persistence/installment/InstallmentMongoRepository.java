package br.com.casellisoftware.budgetmanager.persistence.installment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.YearMonth;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InstallmentMongoRepository extends MongoRepository<InstallmentDocument, String> {

    // Active installments that affect a wallet month.
    // Standalone (sourceWalletId == null): first parcel charges in `sourceEffectiveMonth`
    // itself, so source month is inclusive (`<=`).
    // From-expense (sourceWalletId != null): purchase already counted in the source
    // wallet's month, so first parcel materializes the month after (`<`).
    @Query("{ 'deleted': false, 'lastInstallmentDate': { $gte: ?0 }, "
            + "$or: [ "
            + "  { 'sourceWalletId': null, 'sourceEffectiveMonth': { $lte: ?0 } }, "
            + "  { 'sourceWalletId': { $ne: null }, 'sourceEffectiveMonth': { $lt: ?0 } } "
            + "] }")
    List<InstallmentDocument> findActiveAffecting(YearMonth walletMonth);

    @Query("{ 'ownerId': ?1, 'deleted': false, 'lastInstallmentDate': { $gte: ?0 }, "
            + "$or: [ "
            + "  { 'sourceWalletId': null, 'sourceEffectiveMonth': { $lte: ?0 } }, "
            + "  { 'sourceWalletId': { $ne: null }, 'sourceEffectiveMonth': { $lt: ?0 } } "
            + "] }")
    List<InstallmentDocument> findActiveAffecting(YearMonth walletMonth, String ownerId);

    @Query("{ 'sourceWalletId': ?0, 'deleted': false }")
    List<InstallmentDocument> findBySourceWalletIdAndNotDeleted(String sourceWalletId);

    @Query("{ 'sourceWalletId': ?0, 'ownerId': ?1, 'deleted': false }")
    List<InstallmentDocument> findBySourceWalletIdAndNotDeleted(String sourceWalletId, String ownerId);

    Optional<InstallmentDocument> findByIdAndOwnerId(String id, String ownerId);

    List<InstallmentDocument> findAllByIdInAndOwnerId(Collection<String> ids, String ownerId);

    @Query(value = "{ 'creditCardId': ?0 }", fields = "{ '_id': 1 }")
    List<InstallmentDocument> findIdsByCreditCardId(String creditCardId);

    @Query(value = "{ 'creditCardId': ?0, 'ownerId': ?1 }", fields = "{ '_id': 1 }")
    List<InstallmentDocument> findIdsByCreditCardId(String creditCardId, String ownerId);

    @Query(value = "{ 'creditCardId': ?0, 'deleted': false }", fields = "{ '_id': 1 }")
    List<InstallmentDocument> findIdsByCreditCardIdAndNotDeleted(String creditCardId);

    @Query(value = "{ 'creditCardId': ?0, 'ownerId': ?1, 'deleted': false }", fields = "{ '_id': 1 }")
    List<InstallmentDocument> findIdsByCreditCardIdAndNotDeleted(String creditCardId, String ownerId);

    org.springframework.data.domain.Page<InstallmentDocument> findAllByOwnerId(String ownerId, org.springframework.data.domain.Pageable pageable);
}
