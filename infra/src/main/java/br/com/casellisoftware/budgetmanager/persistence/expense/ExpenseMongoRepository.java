package br.com.casellisoftware.budgetmanager.persistence.expense;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExpenseMongoRepository extends MongoRepository<ExpenseDocument, String> {

    Page<ExpenseDocument> findByWalletId(String walletId, Pageable pageable);

    Page<ExpenseDocument> findByWalletIdAndOwnerId(String walletId, String ownerId, Pageable pageable);

    boolean existsByCreditCardId(String creditCardId);

    boolean existsByCreditCardIdAndOwnerId(String creditCardId, String ownerId);

    Optional<ExpenseDocument> findByInstallmentId(String installmentId);

    Optional<ExpenseDocument> findByIdAndOwnerId(String id, String ownerId);

    Optional<ExpenseDocument> findByInstallmentIdAndOwnerId(String installmentId, String ownerId);

    long deleteByIdAndOwnerId(String id, String ownerId);

    @Query(value = "{ 'creditCardId' : ?0 }", fields = "{ '_id' : 1 }")
    List<ExpenseDocument> findIdsByCreditCardId(String creditCardId);

    @Query(value = "{ 'creditCardId' : ?0, 'ownerId': ?1 }", fields = "{ '_id' : 1 }")
    List<ExpenseDocument> findIdsByCreditCardId(String creditCardId, String ownerId);

    Optional<ExpenseDocument> findBySourcePendingIdAndOwnerId(String sourcePendingId, String ownerId);
}
