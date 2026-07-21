package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.util.List;
import java.util.Optional;

/**
 * Domain port for persisting and querying {@link PluggyConnection} records.
 */
public interface PluggyConnectionRepository {

    PluggyConnection save(PluggyConnection connection);

    List<PluggyConnection> findByOwnerId(String ownerId);

    Optional<PluggyConnection> findByItemIdAndOwnerId(String itemId, String ownerId);

    /**
     * Owner-agnostic lookup by {@code itemId}. Reserved for the webhook flow (phase 2),
     * where the incoming event only carries the Pluggy {@code itemId}.
     */
    Optional<PluggyConnection> findByItemId(String itemId);
}
