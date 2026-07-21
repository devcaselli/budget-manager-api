package br.com.casellisoftware.budgetmanager.pluggy;

import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyAccount;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyItem;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyTransaction;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
    public ConnectToken createConnectToken(String clientUserId, String itemId) {
        Objects.requireNonNull(clientUserId, "clientUserId must not be null");
        ConnectTokenResponse response = withApiKeyRetry(apiKey ->
                restClient.post()
                        .uri("/connect_token")
                        .header("X-API-KEY", apiKey)
                        .body(new ConnectTokenRequest(clientUserId, itemId))
                        .retrieve()
                        .body(ConnectTokenResponse.class));

        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new IllegalStateException("Pluggy /connect_token returned no accessToken");
        }
        return new ConnectToken(response.accessToken());
    }

    @Override
    public PluggyItem getItem(String itemId) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        ItemResponse response = withApiKeyRetry(apiKey ->
                restClient.get()
                        .uri("/items/{itemId}", itemId)
                        .header("X-API-KEY", apiKey)
                        .retrieve()
                        .body(ItemResponse.class));

        if (response == null || response.id() == null) {
            throw new IllegalStateException("Pluggy /items/" + itemId + " returned no item");
        }
        String connectorId = response.connector() != null ? response.connector().id() : null;
        return new PluggyItem(response.id(), connectorId, response.status());
    }

    @Override
    public List<PluggyAccount> listAccounts(String itemId) {
        Objects.requireNonNull(itemId, "itemId must not be null");
        AccountListResponse response = withApiKeyRetry(apiKey ->
                restClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/accounts")
                                .queryParam("itemId", itemId)
                                .build())
                        .header("X-API-KEY", apiKey)
                        .retrieve()
                        .body(AccountListResponse.class));

        if (response == null || response.results() == null) {
            return List.of();
        }
        return response.results().stream()
                .map(item -> new PluggyAccount(item.id(), itemId, item.name(), item.type()))
                .toList();
    }

    /**
     * Safety cap on the number of cursor pages followed per {@link #listTransactions} call.
     * Pluggy's {@code /v2/transactions} contract documents {@code next} as {@code null} when
     * there are no more pages, but this cap guards against an unexpected server-side bug (or a
     * future contract change) turning that into an infinite loop. At 500 items/page this allows
     * up to 25,000 transactions per account/range before bailing out with a clear error.
     */
    private static final int MAX_TRANSACTION_PAGES = 50;

    @Override
    public List<PluggyTransaction> listTransactions(String accountId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(accountId, "accountId must not be null");

        List<PluggyTransaction> accumulated = new ArrayList<>();
        String nextPageQuery = null;
        int page = 0;
        do {
            if (page >= MAX_TRANSACTION_PAGES) {
                throw new IllegalStateException(
                        "Pluggy /v2/transactions exceeded " + MAX_TRANSACTION_PAGES
                                + " pages for accountId=" + accountId + "; aborting to avoid an unbounded loop");
            }
            String cursorQuery = nextPageQuery;
            TransactionListResponse response = withApiKeyRetry(apiKey ->
                    restClient.get()
                            .uri(uriBuilder -> buildTransactionsUri(uriBuilder, accountId, from, to, cursorQuery))
                            .header("X-API-KEY", apiKey)
                            .retrieve()
                            .body(TransactionListResponse.class));

            if (response != null && response.results() != null) {
                response.results().stream()
                        .map(item -> new PluggyTransaction(item.id(), accountId, item.description(), item.amount(),
                                item.currencyCode(), item.date(), item.type(), item.category(), null))
                        .forEach(accumulated::add);
            }

            nextPageQuery = response != null ? response.next() : null;
            page++;
        } while (nextPageQuery != null);

        return List.copyOf(accumulated);
    }

    /**
     * Builds the {@code GET /v2/transactions} URI. On the first call, {@code cursorQuery} is
     * {@code null} and the request is built from {@code accountId}/{@code from}/{@code to}. On
     * subsequent pages, {@code cursorQuery} holds the verbatim {@code next} value from the
     * previous response.
     *
     * <p>Observed shape of {@code next} (per the official cursor-pagination reference,
     * {@code docs.pluggy.ai/reference/transactions-list-by-cursor}): a full relative query
     * string, e.g.
     * {@code "?accountId=562b795d-...&after=MjAyMC0xMC0xNVQwMDowMDowMC4wMDBafGE4NTM0Yzg1LS4uLg=="} —
     * not a bare cursor token. Rather than regex-extracting the {@code after} value (brittle if
     * Pluggy adds/reorders params in {@code next}), we treat {@code next}'s query portion as
     * opaque and feed it to {@link UriBuilder#replaceQuery(String)} as-is, so any current or
     * future params Pluggy includes in {@code next} are preserved verbatim.</p>
     */
    private URI buildTransactionsUri(UriBuilder uriBuilder, String accountId,
                                      LocalDate from, LocalDate to, String cursorQuery) {
        if (cursorQuery != null) {
            // cursorQuery is a leading-"?" relative query string (e.g. "?accountId=...&after=...").
            // Strip the leading "?" and let UriComponentsBuilder parse+re-encode the query params,
            // rather than string-splicing it onto the path ourselves.
            String rawQuery = cursorQuery.startsWith("?") ? cursorQuery.substring(1) : cursorQuery;
            return uriBuilder.path("/v2/transactions").replaceQuery(rawQuery).build();
        }
        var builder = uriBuilder.path("/v2/transactions").queryParam("accountId", accountId);
        if (from != null) {
            builder = builder.queryParam("dateFrom", from.toString());
        }
        if (to != null) {
            builder = builder.queryParam("dateTo", to.toString());
        }
        return builder.build();
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

    /**
     * {@code POST /connect_token} body. {@code clientUserId} is kept flat/top-level (not
     * wrapped in an {@code options} object) because that shape is confirmed working in
     * production for this connector. {@code itemId} is ADDED alongside it, top-level, per
     * {@code docs.pluggy.ai/reference/connect-token-create} — present to scope the token
     * to an existing item for Connect widget update mode, {@code null}/omitted for a
     * brand-new connection.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ConnectTokenRequest(String clientUserId, String itemId) {
    }

    record ConnectTokenResponse(String accessToken) {
    }

    record ItemResponse(String id, ConnectorResponse connector, String status) {
    }

    record ConnectorResponse(String id) {
    }

    record AccountListResponse(List<AccountResponse> results) {
    }

    record AccountResponse(String id, String name, String type, String marketingName, String number,
                            CreditDataResponse creditData) {
    }

    record CreditDataResponse(String brand, String level) {
    }

    /**
     * {@code next}: the cursor to the following page, as a verbatim relative query string
     * (e.g. {@code "?accountId=...&after=..."}), or {@code null} when there are no more pages.
     * Per {@code docs.pluggy.ai/reference/transactions-list-by-cursor}.
     */
    record TransactionListResponse(List<TransactionResponse> results, String next) {
    }

    record TransactionResponse(String id, String description, BigDecimal amount, String currencyCode,
                                LocalDate date, String type, String category) {
    }
}
