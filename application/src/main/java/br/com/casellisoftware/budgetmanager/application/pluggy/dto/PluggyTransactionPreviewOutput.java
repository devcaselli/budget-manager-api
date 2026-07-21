package br.com.casellisoftware.budgetmanager.application.pluggy.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single previewed Pluggy transaction, ready for the frontend review screen.
 *
 * <p>{@code amount} is the raw signed value from Pluggy, so the frontend can render
 * sign/color without additional lookups. {@code isExpense} tells the frontend (and the
 * materialize use case) whether this row is a valid materialization candidate; credit rows
 * are shown for statement transparency but are never turned into an {@code Expense}.</p>
 *
 * @param id             Pluggy transaction id (used as {@code sourcePendingId})
 * @param accountId      Pluggy account id this transaction belongs to
 * @param description    transaction description/merchant
 * @param amount         raw signed amount
 * @param currency       ISO currency code
 * @param date           transaction date
 * @param isExpense      {@code true} when this row is a materialization candidate — see
 *                       {@code PluggyTransaction#isExpense()} for the account-type-aware sign
 *                       rule (inverted for {@code CREDIT} accounts vs. {@code BANK})
 * @param alreadyImported {@code true} if an {@code Expense} with this {@code sourcePendingId} already exists
 */
public record PluggyTransactionPreviewOutput(
        String id,
        String accountId,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate date,
        boolean isExpense,
        boolean alreadyImported
) {
}
