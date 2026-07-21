package br.com.casellisoftware.budgetmanager.persistence.pluggy;

import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PluggyConnectionRepositoryImpl implements PluggyConnectionRepository {

    private final PluggyConnectionMongoRepository mongoRepository;

    @Override
    public PluggyConnection save(PluggyConnection connection) {
        PluggyConnectionDocument doc = new PluggyConnectionDocument(
                connection.getId(),
                connection.getOwnerId(),
                connection.getItemId(),
                connection.getConnectorId(),
                connection.getStatus(),
                connection.getAccountIds(),
                connection.getCreatedAt(),
                connection.getUpdatedAt());
        PluggyConnectionDocument saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public List<PluggyConnection> findByOwnerId(String ownerId) {
        return mongoRepository.findByOwnerId(ownerId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<PluggyConnection> findByItemIdAndOwnerId(String itemId, String ownerId) {
        return mongoRepository.findByItemIdAndOwnerId(itemId, ownerId).map(this::toDomain);
    }

    @Override
    public Optional<PluggyConnection> findByItemId(String itemId) {
        return mongoRepository.findByItemId(itemId).map(this::toDomain);
    }

    private PluggyConnection toDomain(PluggyConnectionDocument doc) {
        return new PluggyConnection(
                doc.getId(),
                doc.getOwnerId(),
                doc.getItemId(),
                doc.getConnectorId(),
                doc.getStatus(),
                doc.getAccountIds(),
                doc.getCreatedAt(),
                doc.getUpdatedAt());
    }
}
