package br.com.casellisoftware.budgetmanager.domain.bullet;

import java.util.List;
import java.util.Optional;

public interface BulletRepository {

    Bullet save(Bullet bullet);

    List<Bullet> saveAll(List<Bullet> bullets);

    Optional<Bullet> findById(String id);
    default Optional<Bullet> findById(String id, String ownerId) {
        return findById(id).filter(bullet -> bullet.getOwnerId().equals(ownerId));
    }
    List<Bullet> findAllByIds(List<String> ids);
    default List<Bullet> findAllByIds(List<String> ids, String ownerId) {
        return findAllByIds(ids).stream()
                .filter(bullet -> bullet.getOwnerId().equals(ownerId))
                .toList();
    }
    List<Bullet> findByWalletId(String walletId);
    default List<Bullet> findByWalletId(String walletId, String ownerId) {
        return findByWalletId(walletId).stream()
                .filter(bullet -> bullet.getOwnerId().equals(ownerId))
                .toList();
    }
    void deleteById(String id);
    default void deleteById(String id, String ownerId) {
        findById(id, ownerId).ifPresent(bullet -> deleteById(id));
    }
}
