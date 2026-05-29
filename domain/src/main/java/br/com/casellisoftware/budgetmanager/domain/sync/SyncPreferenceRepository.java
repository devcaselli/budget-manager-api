package br.com.casellisoftware.budgetmanager.domain.sync;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for persisting and querying {@link SyncPreference} records.
 */
public interface SyncPreferenceRepository {

    SyncPreference save(SyncPreference preference);

    Optional<SyncPreference> findByOwnerId(String ownerId);

    /**
     * Returns all owner ids whose sync preference has {@code enabled = true}.
     * Used by the cron job to determine which owners to include in each sync run.
     *
     * @implNote Time complexity: O(n) where n = total number of sync preference records.
     */
    List<String> findAllEnabledOwnerIds();
}
