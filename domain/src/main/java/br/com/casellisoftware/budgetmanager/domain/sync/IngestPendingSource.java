package br.com.casellisoftware.budgetmanager.domain.sync;

/**
 * Domain port for reading and consuming pending expenses from an external ingest source.
 *
 * <p>The implementation lives in the infra layer ({@code HttpIngestPendingSource}).
 * This interface is kept here so application-layer use cases depend only on the
 * domain abstraction — the underlying transport (HTTP, in-process, etc.) is
 * substitutable without touching use-case logic.</p>
 */
public interface IngestPendingSource {

    /**
     * Lists pending expenses for the given owner.
     *
     * @param ownerId owner identifier (maps to JWT subject)
     * @param limit   maximum items to return per page
     * @param offset  zero-based offset for pagination
     * @return page of pending expenses; never null
     */
    PendingExpensePage listPending(String ownerId, int limit, int offset);

    /**
     * Marks a pending expense as consumed so the ingest-api can remove it.
     * Called after the expense has been successfully persisted in budget-manager.
     *
     * <p>Failure here must be treated as best-effort: the expense has already been
     * saved, and the deduplication guard ({@code sourcePendingId} index) ensures
     * a re-run will skip it safely.</p>
     *
     * @param ownerId owner identifier
     * @param id      pending expense id in the ingest system
     */
    void markConsumed(String ownerId, String id);
}
