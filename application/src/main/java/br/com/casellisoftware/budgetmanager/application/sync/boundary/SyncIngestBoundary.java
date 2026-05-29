package br.com.casellisoftware.budgetmanager.application.sync.boundary;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;

/**
 * Inbound port for triggering an ingest-sync run for a single owner.
 * Consumed by both the REST controller (manual trigger) and the scheduler (cron trigger).
 */
public interface SyncIngestBoundary {

    /**
     * Runs a full sync cycle for the given owner: fetches all pending expenses from
     * ingest-api, resolves credit cards and wallet, materializes each as an {@link br.com.casellisoftware.budgetmanager.domain.expense.Expense},
     * and marks each consumed item in the ingest system.
     *
     * @param ownerId owner identifier (JWT subject)
     * @return summary of the sync run
     */
    SyncReport execute(String ownerId);
}
