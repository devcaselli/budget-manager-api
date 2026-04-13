package br.com.casellisoftware.budgetmanager.domain.bullet;

import java.util.Optional;

public interface BulletRepository {

    Bullet save(Bullet bullet);
    Optional<Bullet> findById(String id);
}
