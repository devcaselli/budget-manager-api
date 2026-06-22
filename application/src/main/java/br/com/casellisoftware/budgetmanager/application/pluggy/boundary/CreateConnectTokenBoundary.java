package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;

/**
 * Inbound port for creating a Pluggy Connect Token for the authenticated owner.
 * Consumed by the REST controller and handed to the frontend Connect widget.
 */
public interface CreateConnectTokenBoundary {

    /**
     * Creates a connect token scoped to the given owner.
     *
     * @param ownerId owner identifier (JWT subject)
     * @return the connect token output to expose to the frontend
     */
    ConnectTokenOutput execute(String ownerId);
}
