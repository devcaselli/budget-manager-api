package br.com.casellisoftware.budgetmanager.domain.payer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayerRepository {

    Payer save(Payer payer);

    Optional<Payer> findById(String id);

    default Optional<Payer> findById(String id, String ownerId) {
        return findById(id).filter(payer -> payer.getOwnerId().equals(ownerId));
    }

    boolean existsById(String id);

    default boolean existsById(String id, String ownerId) {
        return findById(id, ownerId).isPresent();
    }

    List<Payer> findAll(String ownerId);

    default List<Payer> findAllStanding(String ownerId) {
        return findAll(ownerId).stream()
                .filter(payer -> payer.getType() == PayerType.STANDING)
                .toList();
    }

    default List<Payer> findAllByWalletId(String walletId, String ownerId) {
        return findAll(ownerId).stream()
                .filter(payer -> walletId.equals(payer.getWalletId()))
                .toList();
    }

    default List<Payer> findAllByIdsIn(Collection<String> ids, String ownerId) {
        return findAll(ownerId).stream()
                .filter(payer -> ids.contains(payer.getId()))
                .toList();
    }

    void deleteById(String id);

    default void deleteById(String id, String ownerId) {
        findById(id, ownerId).ifPresent(payer -> save(payer.delete()));
    }

    default void deleteAllByWalletId(String walletId, String ownerId) {
        findAllByWalletId(walletId, ownerId).forEach(payer -> save(payer.delete()));
    }
}
