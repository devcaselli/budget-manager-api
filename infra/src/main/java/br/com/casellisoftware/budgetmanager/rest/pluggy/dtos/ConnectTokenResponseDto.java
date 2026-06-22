package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

/**
 * REST response carrying the Pluggy Connect Token for the frontend widget.
 *
 * @param connectToken the short-lived Pluggy Connect Token value
 */
public record ConnectTokenResponseDto(String connectToken) {
}
