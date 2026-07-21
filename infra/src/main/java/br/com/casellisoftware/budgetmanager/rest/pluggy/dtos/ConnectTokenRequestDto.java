package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

/**
 * Optional request body for {@code POST /pluggy/connect-token}.
 *
 * <p>{@code itemId} is nullable: absent/{@code null} requests a token for a brand-new
 * connection (unchanged behavior); present requests a Connect widget <em>update-mode</em>
 * token scoped to that already-connected item (after an ownership check).</p>
 *
 * @param itemId Pluggy item id to scope the token to for update mode, or {@code null}
 */
public record ConnectTokenRequestDto(String itemId) {
}
