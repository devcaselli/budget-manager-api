package br.com.casellisoftware.budgetmanager.persistence.sharing;

import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;
import br.com.casellisoftware.budgetmanager.persistence.sharing.mappers.SharePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ShareRepositoryImpl implements ShareRepository {

    private final ShareMongoRepository shareMongoRepository;
    private final SharePersistenceMapper mapper;

    @Override
    public Share save(Share share) {
        Long version = shareMongoRepository.findById(share.getId())
                .map(ShareDocument::getVersion)
                .orElse(null);
        ShareDocument saved = shareMongoRepository.save(mapper.toDocument(share, version));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Share> findById(String id, String ownerId) {
        return shareMongoRepository.findByIdAndOwnerId(id, ownerId).map(mapper::toDomain);
    }

    @Override
    public Optional<Share> findActiveBySourceId(ShareSourceType type, String sourceId, String ownerId) {
        return shareMongoRepository.findBySourceTypeAndSourceIdAndStatusAndOwnerId(
                        type.name(), sourceId, ShareStatus.ACTIVE.name(), ownerId)
                .map(mapper::toDomain);
    }

    @Override
    public Map<String, Share> findActiveBySourceIds(ShareSourceType type,
                                                    Collection<String> sourceIds,
                                                    String ownerId) {
        if (sourceIds == null || sourceIds.isEmpty()) {
            return Map.of();
        }
        return shareMongoRepository
                .findAllBySourceTypeAndSourceIdInAndStatusAndOwnerId(
                        type.name(), sourceIds, ShareStatus.ACTIVE.name(), ownerId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(Share::getSourceId, Function.identity(), (a, b) -> a));
    }

    @Override
    public List<Share> findAllByOwner(String ownerId) {
        return shareMongoRepository.findAllByOwnerId(ownerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveBySourceId(ShareSourceType type, String sourceId, String ownerId) {
        return shareMongoRepository.existsBySourceTypeAndSourceIdAndStatusAndOwnerId(
                type.name(), sourceId, ShareStatus.ACTIVE.name(), ownerId);
    }

    @Override
    public boolean existsByPayerId(String payerId, String ownerId) {
        return shareMongoRepository.existsByOwnerIdAndQuotasPayerId(ownerId, payerId);
    }

    @Override
    public List<Share> findActiveByPayerId(String payerId, String ownerId) {
        return shareMongoRepository
                .findAllByOwnerIdAndStatusAndQuotasPayerId(ownerId, ShareStatus.ACTIVE.name(), payerId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
