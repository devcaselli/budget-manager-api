package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sync.IngestPendingSource;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpense;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpensePage;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncIngestForOwnerUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 5);
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 19);
    // Fixed clock at 2026-05-19T12:00:00 America/Sao_Paulo = UTC-3 → UTC 15:00
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-19T15:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private IngestPendingSource ingestPendingSource;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ResolveCreditCardForIngestUseCase resolveCreditCard;
    @Mock
    private ResolveIngestWalletUseCase resolveWallet;

    private SyncIngestForOwnerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SyncIngestForOwnerUseCase(
                ingestPendingSource, expenseRepository, resolveCreditCard, resolveWallet, FIXED_CLOCK);
    }

    private PendingExpense pending(String id) {
        return new PendingExpense(id, OWNER, "Nubank", "1234", "Nubank",
                BigDecimal.valueOf(100), "BRL", "Merchant",
                Instant.parse("2026-05-18T20:00:00Z"));
    }

    private Wallet wallet() {
        return new Wallet("w-1", OWNER, "May Budget",
                Money.of(BigDecimal.valueOf(1000), BRL), Money.of(BigDecimal.valueOf(1000), BRL),
                TODAY, null, false, CURRENT_MONTH, WalletState.PRODUCTION, null);
    }

    private CreditCard card() {
        return new CreditCard("cc-1", "Nubank", OWNER);
    }

    private Expense savedExpense(String id) {
        return new Expense(id, "w-1", "cc-1", "Merchant",
                Money.of(BigDecimal.valueOf(100), BRL), Money.of(BigDecimal.valueOf(100), BRL),
                TODAY, List.of(), FlagEnum.NONE, false, null, OWNER, "pending-" + id);
    }

    @Test
    void execute_noPendingItems_returnsEmptyReport() {
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(), 0));

        SyncReport report = useCase.execute(OWNER);

        assertThat(report).isEqualTo(SyncReport.empty());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_singleItem_labelMatch_createsExpense() {
        PendingExpense item = pending("p-1");
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(item), 1));
        when(expenseRepository.findBySourcePendingId("p-1", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(item))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(card(), false));
        Expense saved = savedExpense("1");
        when(expenseRepository.save(any())).thenReturn(saved);

        SyncReport report = useCase.execute(OWNER);

        assertThat(report.created()).isEqualTo(1);
        assertThat(report.skipped()).isZero();
        assertThat(report.fallback()).isZero();
        assertThat(report.errors()).isZero();
        verify(ingestPendingSource).markConsumed(OWNER, "p-1");
    }

    @Test
    void execute_singleItem_fallbackCard_incrementsFallback() {
        PendingExpense item = pending("p-2");
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(item), 1));
        when(expenseRepository.findBySourcePendingId("p-2", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(item))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(
                        new CreditCard("ph-1", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER), true));
        when(expenseRepository.save(any())).thenReturn(savedExpense("2"));

        SyncReport report = useCase.execute(OWNER);

        assertThat(report.created()).isEqualTo(1);
        assertThat(report.fallback()).isEqualTo(1);
    }

    @Test
    void execute_alreadySynced_skipsItem() {
        PendingExpense item = pending("p-3");
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(item), 1));
        when(expenseRepository.findBySourcePendingId("p-3", OWNER))
                .thenReturn(Optional.of(savedExpense("3")));

        SyncReport report = useCase.execute(OWNER);

        assertThat(report.skipped()).isEqualTo(1);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_noWallet_countsAsError_continuesOtherItems() {
        PendingExpense item = pending("p-4");
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(item), 1));
        when(expenseRepository.findBySourcePendingId("p-4", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.empty());

        SyncReport report = useCase.execute(OWNER);

        assertThat(report.errors()).isEqualTo(1);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_markConsumedFails_expenseStillCreated() {
        PendingExpense item = pending("p-5");
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(item), 1));
        when(expenseRepository.findBySourcePendingId("p-5", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(item))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(card(), false));
        when(expenseRepository.save(any())).thenReturn(savedExpense("5"));
        doThrow(new RuntimeException("network error"))
                .when(ingestPendingSource).markConsumed(OWNER, "p-5");

        SyncReport report = useCase.execute(OWNER);

        // expense still counted, markConsumed failure doesn't propagate
        assertThat(report.created()).isEqualTo(1);
        assertThat(report.errors()).isZero();
    }

    @Test
    void execute_pagination_processesAllPages() {
        PendingExpense item1 = pending("p-10");
        PendingExpense item2 = pending("p-11");
        // First page: 1 item, total=2 → second page needed
        when(ingestPendingSource.listPending(OWNER, 100, 0))
                .thenReturn(new PendingExpensePage(List.of(item1), 2));
        when(ingestPendingSource.listPending(OWNER, 100, 1))
                .thenReturn(new PendingExpensePage(List.of(item2), 2));
        when(expenseRepository.findBySourcePendingId(any(), any())).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(any()))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(card(), false));
        when(expenseRepository.save(any())).thenAnswer(inv -> savedExpense("x"));

        SyncReport report = useCase.execute(OWNER);

        assertThat(report.created()).isEqualTo(2);
    }
}
