package br.com.casellisoftware.budgetmanager.persistence.bullet;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.persistence.bullet.mappers.BulletPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Dumb adapter: maps entity ↔ document and delegates to Spring Data. No
 * business decisions, no domain exceptions, no input validation beyond what
 * the {@link Bullet} entity already guarantees.
 */
@Repository
@RequiredArgsConstructor
public class BulletRepositoryImpl implements BulletRepository {

    private final BulletMongoRepository bulletMongoRepository;
    private final BulletPersistenceMapper mapper;

    @Override
    public Bullet save(Bullet bullet) {
        Long version = bulletMongoRepository.findById(bullet.getId())
                .map(BulletDocument::getVersion)
                .orElse(null);
        BulletDocument saved = bulletMongoRepository.save(mapper.toDocument(bullet, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Bullet> findById(String id) {
        return bulletMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Bullet> findAllByIds(List<String> ids) {
        return this.bulletMongoRepository
                .findAllById(ids)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        this.bulletMongoRepository.deleteById(id);
    }
}
