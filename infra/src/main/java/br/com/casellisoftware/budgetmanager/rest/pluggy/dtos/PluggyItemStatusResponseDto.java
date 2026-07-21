package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

/**
 * REST response for {@code GET /pluggy/items/{itemId}/status}, for the frontend to poll
 * until the item reaches {@code UPDATED} after triggering a Connect widget update-mode
 * re-sync.
 *
 * @param status Pluggy item status (e.g. {@code UPDATING}, {@code UPDATED},
 *               {@code LOGIN_ERROR}, {@code OUTDATED}, {@code WAITING_USER_INPUT})
 */
public record PluggyItemStatusResponseDto(String status) {
}
