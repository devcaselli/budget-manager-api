package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Proves the Reserved Budget read-time deduction: a reserved budget created in March
 * deducts from every wallet of March onward (cross-month propagation) but not from prior
 * months, and a variation (2000 -> 1500 from August) resolves the right amount per wallet
 * month (variation history).
 */
@ExtendWith(MockitoExtension.class)
class RepositoryBackedWalletDeductionsQueryReservedBudgetTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String OWNER = "owner-1";

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private ShareRepository shareRepository;
    @Mock
    private ReservedBudgetRepository reservedBudgetRepository;

    private RepositoryBackedWalletDeductionsQuery query;

    @BeforeEach
    void setUp() {
        lenient().when(subscriptionRepository.findActiveFor(any(YearMonth.class), any(SubscriptionState.class), anyString())).thenReturn(List.of());
        lenient().when(installmentRepository.findActiveAffecting(any(YearMonth.class), anyString())).thenReturn(List.of());
        query = new RepositoryBackedWalletDeductionsQuery(subscriptionRepository, installmentRepository, shareRepository, reservedBudgetRepository);
    }

    private static Wallet wallet(YearMonth month) {
        return new Wallet(
                "wallet-" + month, OWNER, "wallet " + month,
                Money.of(BigDecimal.valueOf(5000), BRL), Money.of(BigDecimal.valueOf(5000), BRL),
                month.atDay(1), month.atEndOfMonth(), false,
                month,
                WalletState.PRODUCTION, FlagEnum.NONE
        );
    }

    private static ReservedBudget aluguelWithVariation() {
        // 2000 from March, 1500 from August onward
        return ReservedBudget.create("Aluguel", null, BRL, Money.of("2000.00", BRL), YearMonth.of(2025, 3), FlagEnum.NONE, OWNER)
                .addVersion(YearMonth.of(2025, 8), Money.of("1500.00", BRL));
    }

    @Test
    void forWallet_propagatesFromStartMonthOnward_withVariationHistory() {
        ReservedBudget aluguel = aluguelWithVariation();
        // The repository only returns the reserved budget for months >= startMonth (the query filters startMonth <= month).
        when(reservedBudgetRepository.findActiveFor(YearMonth.of(2025, 2), OWNER)).thenReturn(List.of());
        when(reservedBudgetRepository.findActiveFor(YearMonth.of(2025, 3), OWNER)).thenReturn(List.of(aluguel));
        when(reservedBudgetRepository.findActiveFor(YearMonth.of(2025, 7), OWNER)).thenReturn(List.of(aluguel));
        when(reservedBudgetRepository.findActiveFor(YearMonth.of(2025, 8), OWNER)).thenReturn(List.of(aluguel));

        // February (before startMonth): no deduction.
        assertThat(query.forWallet(wallet(YearMonth.of(2025, 2))).reservedBudgets()).isEqualTo(Money.of("0.00", BRL));
        // March-July: 2000.
        assertThat(query.forWallet(wallet(YearMonth.of(2025, 3))).reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
        assertThat(query.forWallet(wallet(YearMonth.of(2025, 7))).reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
        // August onward: 1500.
        assertThat(query.forWallet(wallet(YearMonth.of(2025, 8))).reservedBudgets()).isEqualTo(Money.of("1500.00", BRL));
    }

    @Test
    void forWallet_remainingSubtractsReservedBudget() {
        ReservedBudget aluguel = aluguelWithVariation();
        when(reservedBudgetRepository.findActiveFor(YearMonth.of(2025, 3), OWNER)).thenReturn(List.of(aluguel));

        Wallet march = wallet(YearMonth.of(2025, 3));
        WalletDeductions deductions = query.forWallet(march);

        // budget 5000 - 2000 reserved = 3000 remaining
        assertThat(deductions.remainingFor(march)).isEqualByComparingTo(BigDecimal.valueOf(3000).setScale(2));
    }

    @Test
    void forWallets_batch_resolvesPerMonthAmount() {
        ReservedBudget aluguel = aluguelWithVariation();
        // Batch path queries findActiveForAny then filters with isApplicable per month.
        when(reservedBudgetRepository.findActiveForAny(any(), org.mockito.ArgumentMatchers.eq(OWNER))).thenReturn(List.of(aluguel));
        lenient().when(installmentRepository.findActiveAffectingAny(any(), anyString())).thenReturn(List.of());

        List<Wallet> wallets = List.of(
                wallet(YearMonth.of(2025, 7)),
                wallet(YearMonth.of(2025, 8))
        );

        var result = query.forWallets(wallets);

        assertThat(result.get("wallet-2025-07").reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
        assertThat(result.get("wallet-2025-08").reservedBudgets()).isEqualTo(Money.of("1500.00", BRL));
    }
}
