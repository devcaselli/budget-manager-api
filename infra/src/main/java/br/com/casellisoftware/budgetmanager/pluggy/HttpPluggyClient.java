package br.com.casellisoftware.budgetmanager.pluggy;

import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * HTTP adapter for {@link PluggyClient} backed by the Pluggy API.
 *
 * <p>Uses Spring's {@link RestClient} (synchronous). Handles the backend-only auth
 * handshake: {@code POST /auth} with clientId + clientSecret yields a short-lived
 * {@code apiKey} (JWT, ~2h) that is then sent as the {@code X-API-KEY} header on
 * subsequent calls.</p>
 *
 * <p>The {@code apiKey} is cached in-memory and lazily (re)fetched: on the first call,
 * and again whenever a call fails with 401/403 (treated as an expired/invalid key).
 * The clientId/clientSecret never leave this adapter and are never logged.</p>
 */
public class HttpPluggyClient implements PluggyClient {

    private static final Logger log = LoggerFactory.getLogger(HttpPluggyClient.class);

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    /** Cached backend apiKey (JWT). Null until first authentication. */
    private final AtomicReference<String> cachedApiKey = new AtomicReference<>();

    public HttpPluggyClient(RestClient restClient, String clientId, String clientSecret) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
    }

    @Override
    public ConnectToken createConnectToken(String clientUserId) {
        Objects.requireNonNull(clientUserId, "clientUserId must not be null");
        ConnectTokenResponse response = withApiKeyRetry(apiKey ->
                restClient.post()
                        .uri("/connect_token")
                        .header("X-API-KEY", apiKey)
                        .body(new ConnectTokenRequest(clientUserId))
                        .retrieve()
                        .body(ConnectTokenResponse.class));

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Pluggy /connect_token returned no accessToken");
        }
        return new ConnectToken(response.accessToken());
    }

    /**
     * Runs an authenticated call, transparently (re)authenticating. If the call fails
     * with 401/403, the cached apiKey is invalidated, a fresh one is obtained, and the
     * call is retried exactly once.
     */
    private <T> T withApiKeyRetry(AuthenticatedCall<T> call) {
        String apiKey = currentApiKey();
        try {
            return call.execute(apiKey);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (isAuthFailure(e.getStatusCode())) {
                log.info("Pluggy apiKey rejected ({}), re-authenticating and retrying once", e.getStatusCode());
                cachedApiKey.set(null);
                return call.execute(authenticate());
            }
            throw e;
        }
    }

    private boolean isAuthFailure(HttpStatusCode status) {
        return status.value() == 401 || status.value() == 403;
    }

    private String currentApiKey() {
        String existing = cachedApiKey.get();
        return existing != null ? existing : authenticate();
    }

    /** Performs {@code POST /auth} and caches the resulting apiKey. */
    private String authenticate() {
        log.debug("Authenticating with Pluggy (POST /auth)");
        AuthResponse response = restClient.post()
                .uri("/auth")
                .body(new AuthRequest(clientId, clientSecret))
                .retrieve()
                .body(AuthResponse.class);

        if (response == null || response.apiKey() == null || response.apiKey().isBlank()) {
            throw new IllegalStateException("Pluggy /auth returned no apiKey");
        }
        cachedApiKey.set(response.apiKey());
        return response.apiKey();
    }

    @FunctionalInterface
    private interface AuthenticatedCall<T> {
        T execute(String apiKey);
    }

    // ---- Internal JSON types ----

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record AuthRequest(String clientId, String clientSecret) {
    }

    record AuthResponse(String apiKey) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ConnectTokenRequest(String clientUserId) {
    }

    record ConnectTokenResponse(String accessToken) {
    }
}
