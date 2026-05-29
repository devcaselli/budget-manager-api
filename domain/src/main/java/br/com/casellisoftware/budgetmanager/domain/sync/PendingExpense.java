package br.com.casellisoftware.budgetmanager.domain.sync;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable value object representing a pending expense received from budget-ingest-api.
 *
 * <p>Fields map directly to the ingest-api GET /expenses/pending response item.
 * {@code cardLabel} is the raw label extracted from the SMS (e.g. "Bradescard") and
 * is used for fuzzy matching against {@code CreditCard.normalizedLabels}.</p>
 */
public record PendingExpense(
        String id,
        String ownerId,
        String bank,
        String cardLast4,
        String cardLabel,
        BigDecimal amount,
        String currency,
        String merchant,
        Instant purchaseAt
) {
}
