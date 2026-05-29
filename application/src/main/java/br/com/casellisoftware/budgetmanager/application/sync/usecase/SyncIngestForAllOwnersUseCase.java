package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncAllOwnersBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncIngestBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Runs ingest-sync for all owners whose {@code syncEnabled = true}.
 * Used exclusively by the cron scheduler.
 *
 * @implNote Time complexity: O(n * p) where n = enabled owners, p = pending items per owner.
 */
public class SyncIngestForAllOwnersUseCase implements SyncAllOwnersBoundary {

    private static final Logger log = LoggerFactory.getLogger(SyncIngestForAllOwnersUseCase.class);

    private final SyncPreferenceRepository syncPreferenceRepository;
    private final SyncIngestBoundary syncIngestBoundary;

    public SyncIngestForAllOwnersUseCase(SyncPreferenceRepository syncPreferenceRepository,
                                         SyncIngestBoundary syncIngestBoundary) {
        this.syncPreferenceRepository = Objects.requireNonNull(syncPreferenceRepository, "syncPreferenceRepository must not be null");
        this.syncIngestBoundary = Objects.requireNonNull(syncIngestBoundary, "syncIngestBoundary must not be null");
    }

    @Override
    public Map<String, SyncReport> execute() {
        List<String> ownerIds = syncPreferenceRepository.findAllEnabledOwnerIds();
        log.info("Cron sync starting for {} enabled owner(s)", ownerIds.size());

        Map<String, SyncReport> results = new LinkedHashMap<>();
        for (String ownerId : ownerIds) {
            try {
                SyncReport report = syncIngestBoundary.execute(ownerId);
                results.put(ownerId, report);
            } catch (Exception e) {
                log.error("Cron sync failed for ownerId={}", ownerId, e);
                results.put(ownerId, SyncReport.empty().incrementErrors());
            }
        }
        return results;
    }
}
