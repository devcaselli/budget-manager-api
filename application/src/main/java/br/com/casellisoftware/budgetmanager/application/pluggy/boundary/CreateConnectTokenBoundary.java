package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;

/**
 * Inbound port for creating a Pluggy Connect Token for the authenticated owner.
 * Consumed by the REST controller and handed to the frontend Connect widget.
 */
public interface CreateConnectTokenBoundary {

    /**
     * Creates a connect token scoped to the given owner (new-connection flow).
     *
     * @param ownerId owner identifier (JWT subject)
     * @return the connect token output to expose to the frontend
     */
    default ConnectTokenOutput execute(String ownerId) {
        return execute(ownerId, null);
    }

    /**
     * Creates a connect token scoped to the given owner, optionally in Connect widget
     * <em>update mode</em> when {@code itemId} is present.
     *
     * @param ownerId owner identifier (JWT subject)
     * @param itemId  Pluggy item id to scope the token to for update mode, or {@code null}
     *                for a brand-new connection
     * @return the connect token output to expose to the frontend
     * @throws br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException
     *         if {@code itemId} is present but not owned by {@code ownerId}
     */
    ConnectTokenOutput execute(String ownerId, String itemId);
}
