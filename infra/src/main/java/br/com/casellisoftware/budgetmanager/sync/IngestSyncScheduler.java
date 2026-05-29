package br.com.casellisoftware.budgetmanager.sync;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncAllOwnersBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Cron-triggered scheduler for ingest-sync.
 *
 * <p>Runs every 15 minutes within the 22:00–23:50 window (America/Sao_Paulo).
 * Only active when {@code app.sync.ingest.cron.enabled=true} (default).</p>
 *
 * <p>Enable/disable at runtime by toggling the property — the bean is conditionally
 * registered at startup.</p>
 */
@Component
@ConditionalOnProperty(name = "app.sync.ingest.cron.enabled", havingValue = "true", matchIfMissing = true)
public class IngestSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestSyncScheduler.class);

    private final SyncAllOwnersBoundary syncAllOwnersBoundary;

    public IngestSyncScheduler(SyncAllOwnersBoundary syncAllOwnersBoundary) {
        this.syncAllOwnersBoundary = Objects.requireNonNull(syncAllOwnersBoundary, "syncAllOwnersBoundary must not be null");
    }

    @Scheduled(cron = "${app.sync.ingest.cron.expression:0 */15 22-23 * * *}", zone = "America/Sao_Paulo")
    public void syncIngest() {
        log.info("Ingest sync cron triggered");
        Map<String, SyncReport> results = syncAllOwnersBoundary.execute();
        results.forEach((ownerId, report) ->
                log.info("Cron sync result ownerId={} created={} skipped={} fallback={} errors={}",
                        ownerId, report.created(), report.skipped(), report.fallback(), report.errors())
        );
    }
}
