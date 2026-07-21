package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.MaterializePluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveCreditCardForIngestUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveIngestWalletUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyTransaction;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Materializes selected Pluggy transactions into {@link Expense} records.
 *
 * <p>Mirrors {@code SyncIngestForOwnerUseCase}'s orchestration: for each target
 * transaction, dedup via {@code sourcePendingId} → resolve wallet (effectiveMonth,
 * 5-tier) → resolve credit card (label match, else {@code card_sync} placeholder) →
 * {@code Expense.create} → save. Per-item failures are caught and counted as errors so
 * one bad transaction does not abort the batch.</p>
 *
 * <p>Credit/income transactions ({@code amount >= 0}) are never materialized: selecting
 * one is a no-op that counts as {@code skipped}, not an error — the caller already saw
 * it flagged {@code isExpense=false} in the preview.</p>
 *
 * @implNote Time complexity: O(a * t + s) where a*t = Pluggy re-fetch (see
 *           {@link FetchPluggyTransactionsForItemUseCase}) and s = size of the selected
 *           set; each selected item is O(1) indexed lookups.
 */
public class MaterializePluggyTransactionsUseCase implements MaterializePluggyTransactionsBoundary {

    private static final Logger log = LoggerFactory.getLogger(MaterializePluggyTransactionsUseCase.class);
    private static final ZoneId BR_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int DEFAULT_RANGE_DAYS = 90;

    private final FetchPluggyTransactionsForItemUseCase fetchTransactions;
    private final ExpenseRepository expenseRepository;
    private final ResolveCreditCardForIngestUseCase resolveCreditCard;
    private final ResolveIngestWalletUseCase resolveWallet;
    private final Clock clock;

    public MaterializePluggyTransactionsUseCase(PluggyClient pluggyClient,
                                                 PluggyConnectionRepository pluggyConnectionRepository,
                                                 ExpenseRepository expenseRepository,
                                                 ResolveCreditCardForIngestUseCase resolveCreditCard,
                                                 ResolveIngestWalletUseCase resolveWallet,
                                                 Clock clock) {
        this.fetchTransactions = new FetchPluggyTransactionsForItemUseCase(pluggyClient, pluggyConnectionRepository);
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.resolveCreditCard = Objects.requireNonNull(resolveCreditCard, "resolveCreditCard must not be null");
        this.resolveWallet = Objects.requireNonNull(resolveWallet, "resolveWallet must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SyncReport execute(String ownerId, String itemId, List<String> transactionIds, boolean all) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
        log.info("Starting Pluggy materialize for ownerId={} itemId={} all={}", ownerId, itemId, all);

        LocalDate today = LocalDate.now(clock.withZone(BR_ZONE));
        LocalDate rangeTo = today;
        LocalDate rangeFrom = rangeTo.minusDays(DEFAULT_RANGE_DAYS);

        List<PluggyTransaction> transactions = fetchTransactions.fetch(ownerId, itemId, rangeFrom, rangeTo);
        List<PluggyTransaction> targets = selectTargets(transactions, transactionIds, all);

        SyncReport report = SyncReport.empty();
        for (PluggyTransaction tx : targets) {
            report = processTransaction(tx, ownerId, today, report);
        }

        log.info("Pluggy materialize complete for ownerId={} itemId={} created={} skipped={} fallback={} errors={}",
                ownerId, itemId, report.created(), report.skipped(), report.fallback(), report.errors());
        return report;
    }

    private List<PluggyTransaction> selectTargets(List<PluggyTransaction> transactions,
                                                   List<String> transactionIds,
                                                   boolean all) {
        if (all) {
            return transactions;
        }
        Set<String> selected = transactionIds != null ? Set.copyOf(transactionIds) : Set.of();
        return transactions.stream().filter(tx -> selected.contains(tx.id())).toList();
    }

    private SyncReport processTransaction(PluggyTransaction tx, String ownerId, LocalDate today, SyncReport report) {
        try {
            if (!tx.isExpense()) {
                log.debug("Skipping non-expense (credit) transactionId={} ownerId={}", tx.id(), ownerId);
                return report.incrementSkipped();
            }

            if (expenseRepository.findBySourcePendingId(tx.id(), ownerId).isPresent()) {
                log.debug("Skipping already-materialized transactionId={} ownerId={}", tx.id(), ownerId);
                return report.incrementSkipped();
            }

            var walletOpt = resolveWallet.resolve(ownerId, today);
            if (walletOpt.isEmpty()) {
                log.warn("No PRODUCTION wallet for ownerId={} transactionId={} — skipping item", ownerId, tx.id());
                return report.incrementErrors();
            }
            Wallet wallet = walletOpt.get();

            var resolved = resolveCreditCard.resolve(ownerId, null);

            Money cost = Money.of(tx.absAmount(), Currency.getInstance(tx.currency()));

            Expense expense = Expense.create(
                    wallet.getId(),
                    resolved.card().getId(),
                    tx.description(),
                    cost,
                    tx.date(),
                    FlagEnum.NONE,
                    false,
                    null,
                    ownerId,
                    tx.id()
            );

            expenseRepository.save(expense);
            log.info("Materialized expense transactionId={} expenseId={} ownerId={} fallback={}",
                    tx.id(), expense.getId(), ownerId, resolved.isFallback());

            return resolved.isFallback() ? report.incrementFallback() : report.incrementCreated();

        } catch (Exception e) {
            log.error("Error processing transactionId={} ownerId={}", tx.id(), ownerId, e);
            return report.incrementErrors();
        }
    }
}
