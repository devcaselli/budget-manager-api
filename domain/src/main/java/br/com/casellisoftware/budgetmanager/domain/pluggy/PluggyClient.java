package br.com.casellisoftware.budgetmanager.domain.pluggy;

/**
 * Outbound port to the Pluggy API.
 *
 * <p>Implementations are responsible for the backend-only authentication handshake
 * ({@code POST /auth} with clientId + clientSecret to obtain a short-lived {@code apiKey})
 * and for never exposing those secrets or the {@code apiKey} to callers. Only the
 * resulting {@link ConnectToken} is safe to return toward the frontend.</p>
 */
public interface PluggyClient {

    /**
     * Creates a Pluggy Connect Token for a frontend Connect session.
     *
     * @param clientUserId stable identifier of the end user (the JWT subject / ownerId),
     *                     forwarded to Pluggy so events can be correlated to the user
     * @return a freshly minted, short-lived connect token
     */
    ConnectToken createConnectToken(String clientUserId);
}
