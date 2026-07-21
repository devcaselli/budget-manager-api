package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyTransactionPreviewOutput;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyTransaction;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Previews the Pluggy transactions for an owner's connected item, without materializing
 * anything. Every transaction on every account of the item is returned — including
 * credits/income — so the review screen shows the full statement; {@code isExpense} and
 * {@code alreadyImported} flags let the frontend disable rows that are not valid
 * materialization candidates (credits) or already materialized (dedup).
 *
 * @implNote Time complexity: O(a * t) for the Pluggy fetch (see
 *           {@link FetchPluggyTransactionsForItemUseCase}) plus O(t) dedup lookups
 *           (one indexed {@code findBySourcePendingId} per transaction).
 */
public class FindPluggyTransactionsUseCase implements FindPluggyTransactionsBoundary {

    private static final ZoneId BR_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int DEFAULT_RANGE_DAYS = 90;

    private final FetchPluggyTransactionsForItemUseCase fetchTransactions;
    private final ExpenseRepository expenseRepository;
    private final Clock clock;

    public FindPluggyTransactionsUseCase(PluggyClient pluggyClient,
                                          PluggyConnectionRepository pluggyConnectionRepository,
                                          ExpenseRepository expenseRepository,
                                          Clock clock) {
        this.fetchTransactions = new FetchPluggyTransactionsForItemUseCase(pluggyClient, pluggyConnectionRepository);
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public List<PluggyTransactionPreviewOutput> execute(String ownerId, String itemId, LocalDate from, LocalDate to) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");

        LocalDate today = LocalDate.now(clock.withZone(BR_ZONE));
        LocalDate rangeTo = to != null ? to : today;
        LocalDate rangeFrom = from != null ? from : rangeTo.minusDays(DEFAULT_RANGE_DAYS);

        List<PluggyTransaction> transactions = fetchTransactions.fetch(ownerId, itemId, rangeFrom, rangeTo);

        return transactions.stream()
                .map(tx -> toPreview(tx, ownerId))
                .toList();
    }

    private PluggyTransactionPreviewOutput toPreview(PluggyTransaction tx, String ownerId) {
        boolean alreadyImported = expenseRepository.findBySourcePendingId(tx.id(), ownerId).isPresent();
        return new PluggyTransactionPreviewOutput(
                tx.id(),
                tx.accountId(),
                tx.description(),
                tx.amount(),
                tx.currency(),
                tx.date(),
                tx.isExpense(),
                alreadyImported);
    }
}
