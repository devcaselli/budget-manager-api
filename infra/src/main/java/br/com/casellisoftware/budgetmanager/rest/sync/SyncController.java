package br.com.casellisoftware.budgetmanager.rest.sync;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.GetSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncIngestBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.UpdateSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncPreferenceOutput;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.rest.sync.dtos.SyncPreferenceRequestDto;
import br.com.casellisoftware.budgetmanager.rest.sync.dtos.SyncPreferenceResponseDto;
import br.com.casellisoftware.budgetmanager.rest.sync.dtos.SyncReportResponseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncIngestBoundary syncIngestBoundary;
    private final GetSyncPreferenceBoundary getSyncPreferenceBoundary;
    private final UpdateSyncPreferenceBoundary updateSyncPreferenceBoundary;

    /**
     * Manually triggers ingest-sync for the authenticated owner.
     * Also bootstraps the owner into the cron tracker if not already present.
     */
    @PostMapping("/ingest")
    public ResponseEntity<SyncReportResponseDto> syncIngest(AuthenticatedUser authenticatedUser) {
        SyncReport report = syncIngestBoundary.execute(authenticatedUser.ownerId());
        return ResponseEntity.ok(new SyncReportResponseDto(
                report.created(), report.skipped(), report.fallback(), report.errors()));
    }

    @GetMapping("/preferences")
    public ResponseEntity<SyncPreferenceResponseDto> getPreference(AuthenticatedUser authenticatedUser) {
        SyncPreferenceOutput output = getSyncPreferenceBoundary.execute(authenticatedUser.ownerId());
        return ResponseEntity.ok(new SyncPreferenceResponseDto(output.ownerId(), output.enabled()));
    }

    @PatchMapping("/preferences")
    public ResponseEntity<SyncPreferenceResponseDto> updatePreference(
            @Valid @RequestBody SyncPreferenceRequestDto request,
            AuthenticatedUser authenticatedUser) {
        SyncPreferenceOutput output = updateSyncPreferenceBoundary.execute(authenticatedUser.ownerId(), request.enabled());
        return ResponseEntity.ok(new SyncPreferenceResponseDto(output.ownerId(), output.enabled()));
    }
}
