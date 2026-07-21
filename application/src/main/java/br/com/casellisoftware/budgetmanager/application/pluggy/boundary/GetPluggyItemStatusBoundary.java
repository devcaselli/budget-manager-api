package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyItemStatusOutput;

/**
 * Inbound port for polling a Pluggy {@code item}'s current sync status. Used by the
 * frontend after triggering an update-mode re-sync via the Connect widget, to poll until
 * the item reaches {@code UPDATED} before re-reading transactions.
 */
public interface GetPluggyItemStatusBoundary {

    /**
     * Fetches the current status of an owner-scoped Pluggy {@code item}.
     *
     * @param ownerId owner identifier (JWT subject)
     * @param itemId  Pluggy item id
     * @return the item's current status
     * @throws br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException
     *         if {@code itemId} is not owned by {@code ownerId}
     */
    PluggyItemStatusOutput execute(String ownerId, String itemId);
}
