package br.com.casellisoftware.budgetmanager.application.sync.dto;

/**
 * Result summary of a single-owner ingest-sync run.
 *
 * @param created   number of expenses successfully materialized
 * @param skipped   number of pending items skipped due to deduplication (already synced)
 * @param fallback  number of items where no label match was found — created under {@code card_sync}
 * @param errors    number of items that failed with an unexpected error
 */
public record SyncReport(int created, int skipped, int fallback, int errors) {

    public static SyncReport empty() {
        return new SyncReport(0, 0, 0, 0);
    }

    public SyncReport incrementCreated() {
        return new SyncReport(created + 1, skipped, fallback, errors);
    }

    public SyncReport incrementSkipped() {
        return new SyncReport(created, skipped + 1, fallback, errors);
    }

    public SyncReport incrementFallback() {
        return new SyncReport(created + 1, skipped, fallback + 1, errors);
    }

    public SyncReport incrementErrors() {
        return new SyncReport(created, skipped, fallback, errors + 1);
    }
}
