package br.com.casellisoftware.budgetmanager.application.pluggy.dto;

import java.time.Instant;
import java.util.List;

/**
 * Output summary of a persisted {@link br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection}.
 *
 * @param id          connection id
 * @param itemId      Pluggy item id
 * @param connectorId Pluggy connector (institution) id
 * @param status      Pluggy item status at the time of registration/last refresh
 * @param accountIds  ids of the accounts belonging to this item
 * @param createdAt   when this connection was first registered
 * @param updatedAt   when this connection was last refreshed
 */
public record PluggyConnectionOutput(
        String id,
        String itemId,
        String connectorId,
        String status,
        List<String> accountIds,
        Instant createdAt,
        Instant updatedAt
) {
}
