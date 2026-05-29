package br.com.casellisoftware.budgetmanager.persistence.extrabudget;

import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.persistence.extrabudget.mappers.ExtraBudgetPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Dumb adapter: maps entity ↔ document and delegates to Spring Data.
 * No business decisions, no domain exceptions, no input validation.
 *
 * <p>findById returns the document even if deleted — required for
 * idempotent delete and potential future reactivation.
 * List queries (findByWalletId, findByBulletId) filter deleted=false
 * at the database level via @Query in ExtraBudgetMongoRepository.</p>
 */
@Repository
@RequiredArgsConstructor
public class ExtraBudgetRepositoryImpl implements ExtraBudgetRepository {

    private final ExtraBudgetMongoRepository mongoRepository;
    private final ExtraBudgetPersistenceMapper mapper;

    @Override
    public ExtraBudget save(ExtraBudget extraBudget) {
        Long version = mongoRepository.findById(extraBudget.getId())
                .map(ExtraBudgetDocument::getVersion)
                .orElse(null);
        ExtraBudgetDocument saved = mongoRepository.save(mapper.toDocument(extraBudget, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ExtraBudget> findById(String id) {
        return mongoRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ExtraBudget> findByWalletId(String walletId, String ownerId) {
        return mongoRepository.findActiveByWalletId(walletId, ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ExtraBudget> findByBulletId(String bulletId, String ownerId) {
        return mongoRepository.findActiveByBulletId(bulletId, ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
