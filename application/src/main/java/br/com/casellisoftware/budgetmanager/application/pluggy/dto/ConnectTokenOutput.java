package br.com.casellisoftware.budgetmanager.application.pluggy.dto;

/**
 * Application-layer output carrying the connect token value safe to send to the frontend.
 *
 * @param connectToken the Pluggy Connect Token value
 */
public record ConnectTokenOutput(String connectToken) {
}
