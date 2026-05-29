package br.com.casellisoftware.budgetmanager.domain.creditcard;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.util.Optional;

public interface CreditCardRepository {

    CreditCard save(CreditCard creditCard);

    Optional<CreditCard> findById(String id);
    default Optional<CreditCard> findById(String id, String ownerId) {
        return findById(id).filter(creditCard -> creditCard.getOwnerId().equals(ownerId));
    }

    boolean existsById(String id);
    default boolean existsById(String id, String ownerId) {
        return findById(id, ownerId).isPresent();
    }

    PageResult<CreditCard> findAll(int page, int size);
    default PageResult<CreditCard> findAll(int page, int size, String ownerId) {
        return findAll(page, size);
    }

    void deleteById(String id);
    default void deleteById(String id, String ownerId) {
        findById(id, ownerId).ifPresent(creditCard -> deleteById(id));
    }

    /**
     * Finds a credit card whose {@code normalizedLabels} list contains the given normalized label,
     * scoped to the owner.
     */
    Optional<CreditCard> findByNormalizedLabel(String normalizedLabel, String ownerId);

    /**
     * Finds a credit card by exact name match, scoped to the owner.
     * Used to locate the {@code card_sync} placeholder.
     */
    Optional<CreditCard> findByName(String name, String ownerId);
}
