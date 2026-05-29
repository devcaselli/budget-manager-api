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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSyncPreferenceUseCaseTest {

    @Mock
    private SyncPreferenceRepository repository;

    private GetSyncPreferenceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetSyncPreferenceUseCase(repository);
    }

    @Test
    void execute_preferenceExists_returnsIt() {
        when(repository.findByOwnerId("owner-1"))
                .thenReturn(Optional.of(new SyncPreference("owner-1", false)));

        SyncPreferenceOutput output = useCase.execute("owner-1");

        assertThat(output.ownerId()).isEqualTo("owner-1");
        assertThat(output.enabled()).isFalse();
    }

    @Test
    void execute_preferenceAbsent_returnsDefault_enabledTrue() {
        when(repository.findByOwnerId("owner-new")).thenReturn(Optional.empty());

        SyncPreferenceOutput output = useCase.execute("owner-new");

        assertThat(output.ownerId()).isEqualTo("owner-new");
        assertThat(output.enabled()).isTrue();
    }
}
