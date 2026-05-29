package br.com.casellisoftware.budgetmanager.application.sync.boundary;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;

import java.util.Map;

/**
 * Inbound port for running sync across all owners with {@code syncEnabled = true}.
 * Used exclusively by the cron scheduler.
 */
public interface SyncAllOwnersBoundary {

    /**
     * Iterates all enabled owners and runs {@link SyncIngestBoundary#execute} for each.
     *
     * @return map of ownerId → SyncReport for each processed owner
     */
    Map<String, SyncReport> execute();
}
