package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.UpdateSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncPreferenceOutput;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreference;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class UpdateSyncPreferenceUseCase implements UpdateSyncPreferenceBoundary {

    private static final Logger log = LoggerFactory.getLogger(UpdateSyncPreferenceUseCase.class);

    private final SyncPreferenceRepository repository;

    public UpdateSyncPreferenceUseCase(SyncPreferenceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public SyncPreferenceOutput execute(String ownerId, boolean enabled) {
        SyncPreference current = repository.findByOwnerId(ownerId)
                .orElse(SyncPreference.defaultFor(ownerId));
        SyncPreference updated = current.withEnabled(enabled);
        SyncPreference saved = repository.save(updated);
        log.info("SyncPreference updated ownerId={} enabled={}", ownerId, enabled);
        return new SyncPreferenceOutput(saved.getOwnerId(), saved.isEnabled());
    }
}
