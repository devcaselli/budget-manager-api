package br.com.casellisoftware.budgetmanager.persistence.sync;

import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreference;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SyncPreferenceRepositoryImpl implements SyncPreferenceRepository {

    private final SyncPreferenceMongoRepository mongoRepository;

    @Override
    public SyncPreference save(SyncPreference preference) {
        SyncPreferenceDocument doc = new SyncPreferenceDocument(preference.getOwnerId(), preference.isEnabled());
        SyncPreferenceDocument saved = mongoRepository.save(doc);
        return new SyncPreference(saved.getOwnerId(), saved.isEnabled());
    }

    @Override
    public Optional<SyncPreference> findByOwnerId(String ownerId) {
        return mongoRepository.findById(ownerId)
                .map(doc -> new SyncPreference(doc.getOwnerId(), doc.isEnabled()));
    }

    @Override
    public List<String> findAllEnabledOwnerIds() {
        return mongoRepository.findAllByEnabledTrue().stream()
                .map(SyncPreferenceDocument::getOwnerId)
                .toList();
    }
}
