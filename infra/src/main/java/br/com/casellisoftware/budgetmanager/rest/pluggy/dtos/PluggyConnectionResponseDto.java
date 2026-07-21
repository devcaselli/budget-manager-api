package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

import java.time.Instant;
import java.util.List;

public record PluggyConnectionResponseDto(
        String id,
        String itemId,
        String connectorId,
        String status,
        List<String> accountIds,
        Instant createdAt,
        Instant updatedAt
) {
}
