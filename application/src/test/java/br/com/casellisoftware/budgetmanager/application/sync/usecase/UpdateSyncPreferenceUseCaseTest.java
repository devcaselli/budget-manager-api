package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncPreferenceOutput;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreference;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSyncPreferenceUseCaseTest {

    @Mock
    private SyncPreferenceRepository repository;

    private UpdateSyncPreferenceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateSyncPreferenceUseCase(repository);
    }

    @Test
    void execute_existingPreference_updatesEnabled() {
        SyncPreference existing = new SyncPreference("owner-1", true);
        SyncPreference updated = existing.withEnabled(false);
        when(repository.findByOwnerId("owner-1")).thenReturn(Optional.of(existing));
        when(repository.save(argThat(p -> !p.isEnabled()))).thenReturn(updated);

        SyncPreferenceOutput output = useCase.execute("owner-1", false);

        assertThat(output.enabled()).isFalse();
        verify(repository).save(argThat(p -> p.getOwnerId().equals("owner-1") && !p.isEnabled()));
    }

    @Test
    void execute_noExistingPreference_createsDefaultThenUpdates() {
        SyncPreference saved = new SyncPreference("owner-new", false);
        when(repository.findByOwnerId("owner-new")).thenReturn(Optional.empty());
        when(repository.save(any(SyncPreference.class))).thenReturn(saved);

        SyncPreferenceOutput output = useCase.execute("owner-new", false);

        assertThat(output.ownerId()).isEqualTo("owner-new");
        assertThat(output.enabled()).isFalse();
    }

    @Test
    void execute_enabledTrue_savesEnabledTrue() {
        SyncPreference existing = new SyncPreference("owner-1", false);
        SyncPreference updated = existing.withEnabled(true);
        when(repository.findByOwnerId("owner-1")).thenReturn(Optional.of(existing));
        when(repository.save(argThat(SyncPreference::isEnabled))).thenReturn(updated);

        SyncPreferenceOutput output = useCase.execute("owner-1", true);

        assertThat(output.enabled()).isTrue();
    }
}
