package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncIngestBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sync.IngestPendingSource;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpense;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpensePage;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Orchestrates a full ingest-sync cycle for a single owner.
 *
 * <p>For each pending expense retrieved from the ingest source:
 * <ol>
 *   <li>Check deduplication via {@code sourcePendingId} — skip if already materialized.</li>
 *   <li>Resolve the target {@link br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard}.</li>
 *   <li>Resolve the target {@link Wallet}.</li>
 *   <li>Create and persist the {@link Expense}.</li>
 *   <li>Mark the pending item as consumed in the ingest system (best-effort).</li>
 * </ol>
 *
 * <p>Wallet resolution failure (no PRODUCTION wallet found) causes the item to be
 * counted as an error and skipped — the sync continues for remaining items.</p>
 *
 * @implNote Time complexity: O(p) where p = number of pending expenses.
 *           Each item involves O(1) lookups (indexed queries).
 */
public class SyncIngestForOwnerUseCase implements SyncIngestBoundary {

    private static final Logger log = LoggerFactory.getLogger(SyncIngestForOwnerUseCase.class);
    private static final ZoneId BR_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final int PAGE_SIZE = 100;

    private final IngestPendingSource ingestPendingSource;
    private final ExpenseRepository expenseRepository;
    private final ResolveCreditCardForIngestUseCase resolveCreditCard;
    private final ResolveIngestWalletUseCase resolveWallet;
    private final Clock clock;

    public SyncIngestForOwnerUseCase(IngestPendingSource ingestPendingSource,
                                     ExpenseRepository expenseRepository,
                                     ResolveCreditCardForIngestUseCase resolveCreditCard,
                                     ResolveIngestWalletUseCase resolveWallet,
                                     Clock clock) {
        this.ingestPendingSource = Objects.requireNonNull(ingestPendingSource, "ingestPendingSource must not be null");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.resolveCreditCard = Objects.requireNonNull(resolveCreditCard, "resolveCreditCard must not be null");
        this.resolveWallet = Objects.requireNonNull(resolveWallet, "resolveWallet must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public SyncReport execute(String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        log.info("Starting ingest sync for ownerId={}", ownerId);

        LocalDate today = LocalDate.now(clock.withZone(BR_ZONE));
        SyncReport report = SyncReport.empty();
        int offset = 0;

        while (true) {
            PendingExpensePage page = ingestPendingSource.listPending(ownerId, PAGE_SIZE, offset);
            if (page.isEmpty()) {
                break;
            }

            for (PendingExpense pending : page.items()) {
                report = processPending(pending, today, report);
            }

            offset += page.items().size();
            if (offset >= page.total()) {
                break;
            }
        }

        log.info("Ingest sync complete for ownerId={} created={} skipped={} fallback={} errors={}",
                ownerId, report.created(), report.skipped(), report.fallback(), report.errors());
        return report;
    }

    private SyncReport processPending(PendingExpense pending, LocalDate today, SyncReport report) {
        try {
            // Deduplication check
            if (expenseRepository.findBySourcePendingId(pending.id(), pending.ownerId()).isPresent()) {
                log.debug("Skipping already-synced pendingId={} ownerId={}", pending.id(), pending.ownerId());
                return report.incrementSkipped();
            }

            // Resolve wallet
            var walletOpt = resolveWallet.resolve(pending.ownerId(), today);
            if (walletOpt.isEmpty()) {
                log.warn("No PRODUCTION wallet for ownerId={} pendingId={} — skipping item", pending.ownerId(), pending.id());
                return report.incrementErrors();
            }
            Wallet wallet = walletOpt.get();

            // Resolve credit card
            var resolved = resolveCreditCard.resolve(pending);

            // Build and save expense
            Money cost = Money.of(pending.amount(), java.util.Currency.getInstance(pending.currency()));
            LocalDate purchaseDate = pending.purchaseAt().atZone(BR_ZONE).toLocalDate();

            Expense expense = Expense.create(
                    wallet.getId(),
                    resolved.card().getId(),
                    pending.merchant(),
                    cost,
                    purchaseDate,
                    FlagEnum.NONE,
                    false,
                    null,
                    pending.ownerId(),
                    pending.id()
            );

            expenseRepository.save(expense);
            log.info("Materialized expense pendingId={} expenseId={} ownerId={} fallback={}",
                    pending.id(), expense.getId(), pending.ownerId(), resolved.isFallback());

            // Mark consumed — best-effort, outside transaction
            try {
                ingestPendingSource.markConsumed(pending.ownerId(), pending.id());
            } catch (Exception e) {
                log.warn("Failed to mark pendingId={} as consumed — dedup will handle on next run", pending.id(), e);
            }

            return resolved.isFallback() ? report.incrementFallback() : report.incrementCreated();

        } catch (Exception e) {
            log.error("Error processing pendingId={} ownerId={}", pending.id(), pending.ownerId(), e);
            return report.incrementErrors();
        }
    }
}
