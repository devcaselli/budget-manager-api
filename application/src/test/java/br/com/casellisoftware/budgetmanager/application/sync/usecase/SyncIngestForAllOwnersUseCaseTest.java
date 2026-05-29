package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncIngestBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncIngestForAllOwnersUseCaseTest {

    @Mock
    private SyncPreferenceRepository syncPreferenceRepository;
    @Mock
    private SyncIngestBoundary syncIngestBoundary;

    private SyncIngestForAllOwnersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncIngestForAllOwnersUseCase(syncPreferenceRepository, syncIngestBoundary);
    }

    @Test
    void execute_noEnabledOwners_returnsEmptyMap() {
        when(syncPreferenceRepository.findAllEnabledOwnerIds()).thenReturn(List.of());

        Map<String, SyncReport> results = useCase.execute();

        assertThat(results).isEmpty();
        verify(syncIngestBoundary, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_twoEnabledOwners_syncsBoth() {
        when(syncPreferenceRepository.findAllEnabledOwnerIds()).thenReturn(List.of("owner-1", "owner-2"));
        SyncReport report1 = new SyncReport(2, 0, 0, 0);
        SyncReport report2 = new SyncReport(1, 1, 0, 0);
        when(syncIngestBoundary.execute("owner-1")).thenReturn(report1);
        when(syncIngestBoundary.execute("owner-2")).thenReturn(report2);

        Map<String, SyncReport> results = useCase.execute();

        assertThat(results).hasSize(2);
        assertThat(results.get("owner-1")).isEqualTo(report1);
        assertThat(results.get("owner-2")).isEqualTo(report2);
    }

    @Test
    void execute_oneOwnerFails_continuesOthers_errorReportInserted() {
        when(syncPreferenceRepository.findAllEnabledOwnerIds()).thenReturn(List.of("owner-fail", "owner-ok"));
        when(syncIngestBoundary.execute("owner-fail")).thenThrow(new RuntimeException("downstream error"));
        SyncReport okReport = new SyncReport(3, 0, 0, 0);
        when(syncIngestBoundary.execute("owner-ok")).thenReturn(okReport);

        Map<String, SyncReport> results = useCase.execute();

        assertThat(results.get("owner-fail").errors()).isEqualTo(1);
        assertThat(results.get("owner-ok")).isEqualTo(okReport);
    }
}
