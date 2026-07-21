package br.com.casellisoftware.budgetmanager.application.pluggy.dto;

/**
 * Application-layer output carrying a Pluggy {@code item}'s current sync status, so the
 * frontend can poll {@code GET /pluggy/items/{itemId}/status} until it reaches
 * {@code UPDATED} after triggering a Connect widget update-mode re-sync.
 *
 * @param status Pluggy item status (e.g. {@code UPDATING}, {@code UPDATED},
 *               {@code LOGIN_ERROR}, {@code OUTDATED}, {@code WAITING_USER_INPUT})
 */
public record PluggyItemStatusOutput(String status) {
}
