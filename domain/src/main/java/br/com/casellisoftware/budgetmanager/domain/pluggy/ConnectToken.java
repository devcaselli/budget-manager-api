package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.util.Objects;

/**
 * A short-lived Pluggy Connect Token, consumed by the frontend Pluggy Connect widget/SDK.
 *
 * <p>This is <strong>not</strong> the backend {@code apiKey}: it is the only token that
 * may safely be handed to the client. It expires after ~30 minutes and is scoped to a
 * single Connect session (optionally to an {@code itemId} for update flows).</p>
 *
 * @param accessToken the connect token value (Pluggy field {@code accessToken})
 */
public record ConnectToken(String accessToken) {

    public ConnectToken {
        Objects.requireNonNull(accessToken, "accessToken must not be null");
        if (accessToken.isBlank()) {
            throw new IllegalArgumentException("accessToken must not be blank");
        }
    }
}
