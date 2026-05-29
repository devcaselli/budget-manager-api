package br.com.casellisoftware.budgetmanager.persistence.bullet;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.persistence.bullet.mappers.BulletPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
    public List<Bullet> saveAll(List<Bullet> bullets) {
        if (bullets.isEmpty()) {
            return List.of();
        }
        List<String> ids = bullets.stream().map(Bullet::getId).toList();
        Map<String, Long> versionById = StreamSupport
                .stream(bulletMongoRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toMap(BulletDocument::getId, BulletDocument::getVersion));

        List<BulletDocument> docs = bullets.stream()
                .map(b -> mapper.toDocument(b, versionById.get(b.getId())))
                .toList();

        return StreamSupport
                .stream(bulletMongoRepository.saveAll(docs).spliterator(), false)
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Bullet> findById(String id) {
        return bulletMongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Bullet> findById(String id, String ownerId) {
        return bulletMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
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
    public List<Bullet> findAllByIds(List<String> ids, String ownerId) {
        return this.bulletMongoRepository
                .findAllByIdInAndOwnerId(ids, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Bullet> findByWalletId(String walletId) {
        return this.bulletMongoRepository
                .findByWalletId(walletId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Bullet> findByWalletId(String walletId, String ownerId) {
        return this.bulletMongoRepository
                .findByWalletIdAndOwnerId(walletId, ownerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(String id) {
        this.bulletMongoRepository.deleteById(id);
    }
}
