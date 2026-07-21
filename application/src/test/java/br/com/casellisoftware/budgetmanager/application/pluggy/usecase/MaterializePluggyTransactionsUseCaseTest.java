package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveCreditCardForIngestUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveIngestWalletUseCase;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyAccount;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyTransaction;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaterializePluggyTransactionsUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final String ITEM_ID = "item-1";
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 5);
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 19);
    // Fixed clock at 2026-05-19T12:00:00 America/Sao_Paulo = UTC-3 -> UTC 15:00
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-19T15:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private PluggyClient pluggyClient;
    @Mock
    private PluggyConnectionRepository pluggyConnectionRepository;
    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ResolveCreditCardForIngestUseCase resolveCreditCard;
    @Mock
    private ResolveIngestWalletUseCase resolveWallet;

    private MaterializePluggyTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new MaterializePluggyTransactionsUseCase(
                pluggyClient, pluggyConnectionRepository, expenseRepository, resolveCreditCard, resolveWallet, FIXED_CLOCK);
    }

    private PluggyConnection connection() {
        return new PluggyConnection("conn-1", OWNER, ITEM_ID, "201", "UPDATED", List.of("acc-1"),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private Wallet wallet() {
        return new Wallet("w-1", OWNER, "May Budget",
                Money.of(BigDecimal.valueOf(1000), BRL), Money.of(BigDecimal.valueOf(1000), BRL),
                TODAY, null, false, CURRENT_MONTH, WalletState.PRODUCTION, null);
    }

    private CreditCard placeholderCard() {
        return new CreditCard("ph-1", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER);
    }

    private void givenConnectionWithTransactions(String accountType, PluggyTransaction... transactions) {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.of(connection()));
        when(pluggyClient.listAccounts(ITEM_ID)).thenReturn(List.of(new PluggyAccount("acc-1", ITEM_ID, "Account", accountType)));
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(transactions));
    }

    @Test
    void execute_unknownConnection_throws() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false))
                .isInstanceOf(PluggyConnectionNotFoundException.class);
    }

    @Test
    void execute_all_materializesEveryExpenseTransaction() {
        PluggyTransaction tx1 = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", TODAY, "DEBIT", null, null);
        PluggyTransaction tx2 = new PluggyTransaction("tx-2", "acc-1", "iFood",
                BigDecimal.valueOf(-30), "BRL", TODAY, "DEBIT", null, null);
        givenConnectionWithTransactions("BANK", tx1, tx2);
        when(expenseRepository.findBySourcePendingId(any(), eq(OWNER))).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(eq(OWNER), isNull()))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(placeholderCard(), true));
        when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncReport report = useCase.execute(OWNER, ITEM_ID, null, true);

        assertThat(report.created()).isEqualTo(2);
        assertThat(report.fallback()).isEqualTo(2);
        assertThat(report.skipped()).isZero();
        assertThat(report.errors()).isZero();
    }

    @Test
    void execute_partialSelection_onlyMaterializesSelectedIds() {
        PluggyTransaction tx1 = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", TODAY, "DEBIT", null, null);
        PluggyTransaction tx2 = new PluggyTransaction("tx-2", "acc-1", "iFood",
                BigDecimal.valueOf(-30), "BRL", TODAY, "DEBIT", null, null);
        givenConnectionWithTransactions("BANK", tx1, tx2);
        when(expenseRepository.findBySourcePendingId("tx-1", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(eq(OWNER), isNull()))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(placeholderCard(), true));
        when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.created()).isEqualTo(1);
        verify(expenseRepository, never()).findBySourcePendingId(eq("tx-2"), any());
    }

    @Test
    void execute_dedupAlreadyImported_skips() {
        PluggyTransaction tx = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", TODAY, "DEBIT", null, null);
        givenConnectionWithTransactions("BANK", tx);
        Expense existing = new Expense("exp-1", "w-1", "cc-1", "Uber",
                Money.of(BigDecimal.valueOf(20), BRL), Money.of(BigDecimal.valueOf(20), BRL),
                TODAY, List.of(), FlagEnum.NONE, false, null, OWNER, "tx-1");
        when(expenseRepository.findBySourcePendingId("tx-1", OWNER)).thenReturn(Optional.of(existing));

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.created()).isZero();
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_bankIncomeTransactionSelected_skipsWithoutError() {
        PluggyTransaction credit = new PluggyTransaction("tx-1", "acc-1", "Salary",
                BigDecimal.valueOf(3000), "BRL", TODAY, "CREDIT", null, null);
        givenConnectionWithTransactions("BANK", credit);

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.errors()).isZero();
        verify(expenseRepository, never()).findBySourcePendingId(any(), any());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_creditCardPurchase_positiveAmount_isMaterialized() {
        // Regression case: real production Pluggy credit-card connectors send genuine card
        // purchases (e.g. Netflix, Uber) with a POSITIVE amount. The account-type-aware rule
        // must materialize them.
        PluggyTransaction tx = new PluggyTransaction("tx-1", "acc-1", "Netflix",
                BigDecimal.valueOf(72.80), "BRL", TODAY, "CREDIT", "Entertainment", null);
        givenConnectionWithTransactions("CREDIT", tx);
        when(expenseRepository.findBySourcePendingId("tx-1", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        when(resolveCreditCard.resolve(eq(OWNER), isNull()))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(placeholderCard(), true));
        when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.created()).isEqualTo(1);
        assertThat(report.skipped()).isZero();
        assertThat(report.errors()).isZero();
    }

    @Test
    void execute_creditCardBillPayment_negativeAmount_skipsWithoutError() {
        // A bill payment / refund on a CREDIT account arrives negative and must never be
        // materialized (it's a settlement, not a purchase — importing it would double-count
        // against the individual card purchases).
        PluggyTransaction billPayment = new PluggyTransaction("tx-1", "acc-1", "PAG BOLETO BANCARIO",
                BigDecimal.valueOf(-4211), "BRL", TODAY, "DEBIT", null, null);
        givenConnectionWithTransactions("CREDIT", billPayment);

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.created()).isZero();
        assertThat(report.errors()).isZero();
        verify(expenseRepository, never()).findBySourcePendingId(any(), any());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_creditCardPaymentCategory_anySign_skipsWithoutError() {
        // "Credit card payment" is a bill payment / transfer-out, not a purchase — excluding
        // it prevents double-counting card purchases that also arrive from the card account.
        PluggyTransaction billPayment = new PluggyTransaction("tx-1", "acc-1", "Payment received",
                BigDecimal.valueOf(-500), "BRL", TODAY, "DEBIT", "Credit card payment", null);
        givenConnectionWithTransactions("CREDIT", billPayment);

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.skipped()).isEqualTo(1);
        assertThat(report.created()).isZero();
        assertThat(report.errors()).isZero();
        verify(expenseRepository, never()).findBySourcePendingId(any(), any());
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_noWallet_countsAsError_continuesOtherItems() {
        PluggyTransaction tx1 = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", TODAY, "DEBIT", null, null);
        PluggyTransaction tx2 = new PluggyTransaction("tx-2", "acc-1", "iFood",
                BigDecimal.valueOf(-30), "BRL", TODAY, "DEBIT", null, null);
        givenConnectionWithTransactions("BANK", tx1, tx2);
        when(expenseRepository.findBySourcePendingId(any(), eq(OWNER))).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.empty());

        SyncReport report = useCase.execute(OWNER, ITEM_ID, null, true);

        assertThat(report.errors()).isEqualTo(2);
        verify(expenseRepository, never()).save(any());
    }

    @Test
    void execute_labelMatchCard_incrementsCreatedNotFallback() {
        PluggyTransaction tx = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", TODAY, "DEBIT", null, null);
        givenConnectionWithTransactions("BANK", tx);
        when(expenseRepository.findBySourcePendingId("tx-1", OWNER)).thenReturn(Optional.empty());
        when(resolveWallet.resolve(OWNER, TODAY)).thenReturn(Optional.of(wallet()));
        CreditCard matched = new CreditCard("cc-1", "Nubank", OWNER);
        when(resolveCreditCard.resolve(eq(OWNER), isNull()))
                .thenReturn(new ResolveCreditCardForIngestUseCase.ResolvedCard(matched, false));
        when(expenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SyncReport report = useCase.execute(OWNER, ITEM_ID, List.of("tx-1"), false);

        assertThat(report.created()).isEqualTo(1);
        assertThat(report.fallback()).isZero();
    }
}
