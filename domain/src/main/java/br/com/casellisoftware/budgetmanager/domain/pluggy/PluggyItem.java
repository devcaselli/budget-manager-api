package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.util.Objects;

/**
 * A Pluggy {@code item} — represents a single end-user connection to a financial
 * institution (bank) via a connector.
 *
 * @param id          Pluggy item id
 * @param connectorId id of the connector (institution) this item was created against
 * @param status      Pluggy item status (e.g. {@code UPDATED}, {@code UPDATING}, {@code LOGIN_ERROR})
 */
public record PluggyItem(String id, String connectorId, String status) {

    public PluggyItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
    }
}
