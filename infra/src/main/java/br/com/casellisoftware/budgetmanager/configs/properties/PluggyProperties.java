package br.com.casellisoftware.budgetmanager.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Pluggy integration.
 *
 * <p>Bound from {@code app.pluggy.*} in application YAML. {@code clientId} and
 * {@code clientSecret} are backend-only secrets used for the {@code POST /auth}
 * handshake; they must never be exposed to the frontend nor logged.</p>
 *
 * @param baseUrl       Pluggy API base URL (e.g. {@code https://api.pluggy.ai})
 * @param clientId      Pluggy client id (secret)
 * @param clientSecret  Pluggy client secret (secret)
 */
@ConfigurationProperties(prefix = "app.pluggy")
public record PluggyProperties(
        String baseUrl,
        String clientId,
        String clientSecret
) {
    public PluggyProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.pluggy.base-url must not be blank");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("app.pluggy.client-id must not be blank");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("app.pluggy.client-secret must not be blank");
        }
    }
}
