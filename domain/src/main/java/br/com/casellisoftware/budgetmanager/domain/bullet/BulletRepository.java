package br.com.casellisoftware.budgetmanager.domain.bullet;

import java.util.List;
import java.util.Optional;

public interface BulletRepository {

    Bullet save(Bullet bullet);
    Optional<Bullet> findById(String id);
    List<Bullet> findAllByIds(List<String> ids);
}
