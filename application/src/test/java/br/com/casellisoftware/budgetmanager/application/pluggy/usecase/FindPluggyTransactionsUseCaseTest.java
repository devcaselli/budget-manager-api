package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyTransactionPreviewOutput;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPluggyTransactionsUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final String ITEM_ID = "item-1";
    private static final Currency BRL = Currency.getInstance("BRL");
    // Fixed clock at 2026-05-19T12:00:00 America/Sao_Paulo = UTC-3 -> UTC 15:00
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-19T15:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private PluggyClient pluggyClient;
    @Mock
    private PluggyConnectionRepository pluggyConnectionRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    private FindPluggyTransactionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindPluggyTransactionsUseCase(pluggyClient, pluggyConnectionRepository, expenseRepository, FIXED_CLOCK);
    }

    private PluggyConnection connection(List<String> accountIds) {
        return new PluggyConnection("conn-1", OWNER, ITEM_ID, "201", "UPDATED", accountIds,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }

    private void givenAccountTypes(String... accountIdTypePairs) {
        List<PluggyAccount> accounts = new java.util.ArrayList<>();
        for (int i = 0; i < accountIdTypePairs.length; i += 2) {
            accounts.add(new PluggyAccount(accountIdTypePairs[i], ITEM_ID, "Account", accountIdTypePairs[i + 1]));
        }
        when(pluggyClient.listAccounts(ITEM_ID)).thenReturn(accounts);
    }

    @Test
    void execute_unknownConnection_throws() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OWNER, ITEM_ID, null, null))
                .isInstanceOf(PluggyConnectionNotFoundException.class);
    }

    @Test
    void execute_bankSpend_notAlreadyImported_marksIsExpenseTrue_alreadyImportedFalse() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1"))));
        givenAccountTypes("acc-1", "BANK");
        PluggyTransaction tx = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-45.90), "BRL", LocalDate.of(2026, 5, 10), "DEBIT", null, null);
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(tx));
        when(expenseRepository.findBySourcePendingId("tx-1", OWNER)).thenReturn(Optional.empty());

        List<PluggyTransactionPreviewOutput> result = useCase.execute(OWNER, ITEM_ID, null, null);

        assertThat(result).hasSize(1);
        PluggyTransactionPreviewOutput preview = result.get(0);
        assertThat(preview.id()).isEqualTo("tx-1");
        assertThat(preview.isExpense()).isTrue();
        assertThat(preview.alreadyImported()).isFalse();
    }

    @Test
    void execute_bankIncome_positiveAmount_marksIsExpenseFalse() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1"))));
        givenAccountTypes("acc-1", "BANK");
        PluggyTransaction credit = new PluggyTransaction("tx-2", "acc-1", "Salary",
                BigDecimal.valueOf(3000), "BRL", LocalDate.of(2026, 5, 5), "CREDIT", null, null);
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(credit));
        when(expenseRepository.findBySourcePendingId("tx-2", OWNER)).thenReturn(Optional.empty());

        List<PluggyTransactionPreviewOutput> result = useCase.execute(OWNER, ITEM_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isExpense()).isFalse();
        assertThat(result.get(0).amount()).isEqualByComparingTo(BigDecimal.valueOf(3000));
    }

    @Test
    void execute_creditCardPurchase_positiveAmount_marksIsExpenseTrue() {
        // Confirmed against REAL production Pluggy data: a credit-card purchase arrives with
        // a POSITIVE amount; direction must consider the owning account's type.
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1"))));
        givenAccountTypes("acc-1", "CREDIT");
        PluggyTransaction tx = new PluggyTransaction("tx-4", "acc-1", "Netflix",
                BigDecimal.valueOf(72.80), "BRL", LocalDate.of(2026, 5, 15), "CREDIT", "Entertainment", null);
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(tx));
        when(expenseRepository.findBySourcePendingId("tx-4", OWNER)).thenReturn(Optional.empty());

        List<PluggyTransactionPreviewOutput> result = useCase.execute(OWNER, ITEM_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isExpense()).isTrue();
    }

    @Test
    void execute_creditCardBillPayment_negativeAmount_marksIsExpenseFalse() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1"))));
        givenAccountTypes("acc-1", "CREDIT");
        PluggyTransaction tx = new PluggyTransaction("tx-5", "acc-1", "PAG BOLETO BANCARIO",
                BigDecimal.valueOf(-4211), "BRL", LocalDate.of(2026, 5, 16), "DEBIT", null, null);
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(tx));
        when(expenseRepository.findBySourcePendingId("tx-5", OWNER)).thenReturn(Optional.empty());

        List<PluggyTransactionPreviewOutput> result = useCase.execute(OWNER, ITEM_ID, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isExpense()).isFalse();
    }

    @Test
    void execute_alreadyMaterializedTransaction_marksAlreadyImportedTrue() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1"))));
        givenAccountTypes("acc-1", "BANK");
        PluggyTransaction tx = new PluggyTransaction("tx-3", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", LocalDate.of(2026, 5, 12), "DEBIT", null, null);
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(tx));
        Expense existing = new Expense("exp-1", "w-1", "cc-1", "Uber",
                Money.of(BigDecimal.valueOf(20), BRL), Money.of(BigDecimal.valueOf(20), BRL),
                LocalDate.of(2026, 5, 12), List.of(), FlagEnum.NONE, false, null, OWNER, "tx-3");
        when(expenseRepository.findBySourcePendingId("tx-3", OWNER)).thenReturn(Optional.of(existing));

        List<PluggyTransactionPreviewOutput> result = useCase.execute(OWNER, ITEM_ID, null, null);

        assertThat(result.get(0).alreadyImported()).isTrue();
    }

    @Test
    void execute_multipleAccounts_aggregatesTransactionsFromAll() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1", "acc-2"))));
        givenAccountTypes("acc-1", "BANK", "acc-2", "BANK");
        PluggyTransaction tx1 = new PluggyTransaction("tx-1", "acc-1", "Uber",
                BigDecimal.valueOf(-20), "BRL", LocalDate.of(2026, 5, 12), "DEBIT", null, null);
        PluggyTransaction tx2 = new PluggyTransaction("tx-2", "acc-2", "iFood",
                BigDecimal.valueOf(-30), "BRL", LocalDate.of(2026, 5, 13), "DEBIT", null, null);
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of(tx1));
        when(pluggyClient.listTransactions(eq("acc-2"), any(), any())).thenReturn(List.of(tx2));
        when(expenseRepository.findBySourcePendingId(any(), eq(OWNER))).thenReturn(Optional.empty());

        List<PluggyTransactionPreviewOutput> result = useCase.execute(OWNER, ITEM_ID, null, null);

        assertThat(result).extracting(PluggyTransactionPreviewOutput::id)
                .containsExactlyInAnyOrder("tx-1", "tx-2");
    }

    @Test
    void execute_nullRange_defaultsToLast90Days() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER))
                .thenReturn(Optional.of(connection(List.of("acc-1"))));
        givenAccountTypes("acc-1", "BANK");
        when(pluggyClient.listTransactions(eq("acc-1"), any(), any())).thenReturn(List.of());

        useCase.execute(OWNER, ITEM_ID, null, null);

        LocalDate today = LocalDate.of(2026, 5, 19);
        verify(pluggyClient).listTransactions("acc-1", today.minusDays(90), today);
    }
}
