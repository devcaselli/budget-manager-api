package br.com.casellisoftware.budgetmanager.sync;

import br.com.casellisoftware.budgetmanager.domain.sync.IngestPendingSource;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpense;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpensePage;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * HTTP adapter for {@link IngestPendingSource} backed by budget-ingest-api.
 *
 * <p>Uses Spring's {@link RestClient} (synchronous, introduced in Spring 6.1).
 * Retry is intentionally not implemented here — the caller (use case) is resilient
 * to partial failures and the dedup guard on {@code sourcePendingId} handles re-runs.</p>
 */
public class HttpIngestPendingSource implements IngestPendingSource {

    private static final Logger log = LoggerFactory.getLogger(HttpIngestPendingSource.class);

    private final RestClient restClient;

    public HttpIngestPendingSource(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient must not be null");
    }

    @Override
    public PendingExpensePage listPending(String ownerId, int limit, int offset) {
        log.debug("Fetching pending expenses ownerId={} limit={} offset={}", ownerId, limit, offset);
        IngestPendingResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/expenses/pending")
                        .queryParam("limit", limit)
                        .queryParam("offset", offset)
                        .build())
                .header("X-Owner-Id", ownerId)
                .retrieve()
                .body(IngestPendingResponse.class);

        if (response == null || response.items() == null) {
            return new PendingExpensePage(List.of(), 0);
        }

        List<PendingExpense> items = response.items().stream()
                .map(item -> new PendingExpense(
                        item.id(),
                        ownerId,
                        item.bank(),
                        item.cardLast4(),
                        item.cardLabel(),
                        item.amount(),
                        item.currency(),
                        item.merchant(),
                        item.purchaseAt()
                ))
                .toList();

        return new PendingExpensePage(items, response.total());
    }

    @Override
    public void markConsumed(String ownerId, String id) {
        log.debug("Marking consumed pendingId={} ownerId={}", id, ownerId);
        restClient.delete()
                .uri("/expenses/{id}", id)
                .header("X-Owner-Id", ownerId)
                .retrieve()
                .toBodilessEntity();
    }

    // ---- Internal JSON response types ----

    record IngestPendingResponse(List<IngestPendingItem> items, long total) {
    }

    record IngestPendingItem(
            String id,
            String bank,
            @JsonProperty("card_last4") String cardLast4,
            @JsonProperty("card_label") String cardLabel,
            BigDecimal amount,
            String currency,
            String merchant,
            @JsonProperty("purchase_at") Instant purchaseAt
    ) {
    }
}
