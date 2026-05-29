package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.GetSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncPreferenceOutput;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreference;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;

import java.util.Objects;

public class GetSyncPreferenceUseCase implements GetSyncPreferenceBoundary {

    private final SyncPreferenceRepository repository;

    public GetSyncPreferenceUseCase(SyncPreferenceRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public SyncPreferenceOutput execute(String ownerId) {
        SyncPreference preference = repository.findByOwnerId(ownerId)
                .orElse(SyncPreference.defaultFor(ownerId));
        return new SyncPreferenceOutput(preference.getOwnerId(), preference.isEnabled());
    }
}
