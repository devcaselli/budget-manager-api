package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies rule 4 of the Vínculos feature: a subscription or installment linked to a
 * reserved budget is excluded from direct wallet deductions from {@code fromMonth} onward.
 *
 * <p>Before {@code fromMonth} the item still deducts directly. From {@code fromMonth}
 * onward the RB ceiling already covers its cost — counting it again would double-count.</p>
 */
@ExtendWith(MockitoExtension.class)
class RepositoryBackedWalletDeductionsQueryLinkedExclusionTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String OWNER = "owner-1";
    private static final YearMonth MAY_2025 = YearMonth.of(2025, 5);
    private static final YearMonth JUN_2025 = YearMonth.of(2025, 6);
    private static final YearMonth JUL_2025 = YearMonth.of(2025, 7);

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
        lenient().when(shareRepository.findActiveBySourceIds(any(), any(), anyString())).thenReturn(Map.of());
        query = new RepositoryBackedWalletDeductionsQuery(
                subscriptionRepository, installmentRepository, shareRepository, reservedBudgetRepository);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static Wallet wallet(YearMonth month) {
        return new Wallet(
                "wallet-" + month, OWNER, "Wallet " + month,
                Money.of(BigDecimal.valueOf(5000), BRL), Money.of(BigDecimal.valueOf(5000), BRL),
                month.atDay(1), month.atEndOfMonth(), false,
                month,
                WalletState.PRODUCTION, FlagEnum.NONE
        );
    }

    private static Subscription subscription(String name) {
        return Subscription.create(name, BRL, Money.of("500.00", BRL),
                YearMonth.of(2025, 1), SubscriptionState.PRODUCTION, FlagEnum.NONE, OWNER, "cc-test");
    }

    private static Installment installment(String name, YearMonth sourceMonth) {
        return Installment.create(
                name,
                Money.of("6000.00", BRL),
                Money.of("1000.00", BRL),
                6,
                LocalDate.of(2025, 5, 1),
                "cc-test",
                OWNER,
                sourceMonth,
                FlagEnum.NONE
        );
    }

    private static ReservedBudget rbWithLink(ReservedBudgetLinkSourceType type,
                                              String sourceId,
                                              YearMonth fromMonth) {
        ReservedBudget rb = ReservedBudget.create(
                "RB", null, BRL, Money.of("2000.00", BRL), YearMonth.of(2025, 1), FlagEnum.NONE, OWNER);
        return rb.addLink(new ReservedBudgetLink(type, sourceId, fromMonth));
    }

    // ─── subscription exclusion ───────────────────────────────────────────────

    @Test
    void linkedSubscription_fromMonth_isExcludedFromDirectDeductions() {
        Subscription netflix = subscription("Netflix");
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025);

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of());

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        // Netflix excluded → sub total = 0, RB deducts 2000
        assertThat(deductions.subscriptions()).isEqualTo(Money.of("0.00", BRL));
        assertThat(deductions.reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void linkedSubscription_beforeFromMonth_stillDeductsDirectly() {
        Subscription netflix = subscription("Netflix");
        // link starts June, wallet is May → not yet applicable
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025);

        when(reservedBudgetRepository.findActiveFor(MAY_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(MAY_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(installmentRepository.findActiveAffecting(MAY_2025, OWNER)).thenReturn(List.of());

        WalletDeductions deductions = query.forWallet(wallet(MAY_2025));

        // Netflix NOT excluded in May → sub total = 500
        assertThat(deductions.subscriptions()).isEqualTo(Money.of("500.00", BRL));
    }

    @Test
    void linkedSubscription_exactlyAtFromMonth_isExcluded() {
        Subscription netflix = subscription("Netflix");
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025);

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of());

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        assertThat(deductions.subscriptions()).isEqualTo(Money.of("0.00", BRL));
    }

    @Test
    void twoSubscriptions_onlyLinkedOneExcluded() {
        Subscription netflix = subscription("Netflix");
        Subscription spotify = subscription("Spotify");
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025);

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix, spotify));
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of());

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        // Only Spotify deducts directly (500); Netflix absorbed by RB
        assertThat(deductions.subscriptions()).isEqualTo(Money.of("500.00", BRL));
        assertThat(deductions.reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void noLinks_noExclusion_allSubscriptionsDeduct() {
        Subscription netflix = subscription("Netflix");
        ReservedBudget rb = ReservedBudget.create(
                "RB", null, BRL, Money.of("2000.00", BRL), YearMonth.of(2025, 1), FlagEnum.NONE, OWNER);

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of());

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        assertThat(deductions.subscriptions()).isEqualTo(Money.of("500.00", BRL));
    }

    // ─── installment exclusion ────────────────────────────────────────────────

    @Test
    void linkedInstallment_fromMonth_isExcludedFromDirectDeductions() {
        Installment notebook = installment("Notebook", MAY_2025);
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.INSTALLMENT, notebook.getId(), JUN_2025);

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of());
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of(notebook));

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        // Notebook excluded → installments total = 0
        assertThat(deductions.installments()).isEqualTo(Money.of("0.00", BRL));
        assertThat(deductions.reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void linkedInstallment_beforeFromMonth_stillDeductsDirectly() {
        Installment notebook = installment("Notebook", MAY_2025);
        // link starts July, wallet is June → not yet applicable
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.INSTALLMENT, notebook.getId(), JUL_2025);

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of());
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of(notebook));

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        // Notebook still deducts in June
        assertThat(deductions.installments()).isEqualTo(Money.of("1000.00", BRL));
    }

    // ─── mixed sub + installment exclusion ───────────────────────────────────

    @Test
    void linkedSubAndInstallment_bothExcluded_rbAbsorbsBoth() {
        Subscription netflix = subscription("Netflix");
        Installment notebook = installment("Notebook", MAY_2025);

        ReservedBudget rb = ReservedBudget.create(
                "RB", null, BRL, Money.of("3000.00", BRL), YearMonth.of(2025, 1), FlagEnum.NONE, OWNER)
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025))
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, notebook.getId(), JUN_2025));

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of(notebook));

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        assertThat(deductions.subscriptions()).isEqualTo(Money.of("0.00", BRL));
        assertThat(deductions.installments()).isEqualTo(Money.of("0.00", BRL));
        assertThat(deductions.reservedBudgets()).isEqualTo(Money.of("3000.00", BRL));
    }

    // ─── forWallets batch path ─────────────────────────────────────────────

    @Test
    void forWallets_linkedSubscription_excludedInBatchPath() {
        Subscription netflix = subscription("Netflix");
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025);

        // batch path uses findActiveForAny (multi-month) + findActiveAffectingAny
        when(reservedBudgetRepository.findActiveForAny(any(), anyString())).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        lenient().when(installmentRepository.findActiveAffectingAny(any(), anyString())).thenReturn(List.of());

        Map<String, WalletDeductions> result = query.forWallets(List.of(wallet(JUN_2025)));

        WalletDeductions deductions = result.get("wallet-" + JUN_2025);
        assertThat(deductions.subscriptions()).isEqualTo(Money.of("0.00", BRL));
        assertThat(deductions.reservedBudgets()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void forWallets_linkedSubscription_beforeFromMonth_notExcluded() {
        Subscription netflix = subscription("Netflix");
        // link starts July, wallets are June
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUL_2025);

        when(reservedBudgetRepository.findActiveForAny(any(), anyString())).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        lenient().when(installmentRepository.findActiveAffectingAny(any(), anyString())).thenReturn(List.of());

        Map<String, WalletDeductions> result = query.forWallets(List.of(wallet(JUN_2025)));

        // June < July → netflix still deducts directly
        assertThat(result.get("wallet-" + JUN_2025).subscriptions()).isEqualTo(Money.of("500.00", BRL));
    }

    @Test
    void forWallets_multipleMonths_exclusionAppliesPerMonth() {
        Subscription netflix = subscription("Netflix");
        // link starts June: May → still direct; Jun+ → excluded
        ReservedBudget rb = rbWithLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025);

        when(reservedBudgetRepository.findActiveForAny(any(), anyString())).thenReturn(List.of(rb));
        when(subscriptionRepository.findActiveFor(MAY_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        lenient().when(installmentRepository.findActiveAffectingAny(any(), anyString())).thenReturn(List.of());

        Map<String, WalletDeductions> result = query.forWallets(List.of(wallet(MAY_2025), wallet(JUN_2025)));

        // May: link not yet applicable → deducts 500
        assertThat(result.get("wallet-" + MAY_2025).subscriptions()).isEqualTo(Money.of("500.00", BRL));
        // June: link applicable → excluded, deducts 0
        assertThat(result.get("wallet-" + JUN_2025).subscriptions()).isEqualTo(Money.of("0.00", BRL));
    }

    @Test
    void noActiveReservedBudgets_noExclusion() {
        Subscription netflix = subscription("Netflix");

        when(reservedBudgetRepository.findActiveFor(JUN_2025, OWNER)).thenReturn(List.of());
        when(subscriptionRepository.findActiveFor(JUN_2025, SubscriptionState.PRODUCTION, OWNER))
                .thenReturn(List.of(netflix));
        when(installmentRepository.findActiveAffecting(JUN_2025, OWNER)).thenReturn(List.of());

        WalletDeductions deductions = query.forWallet(wallet(JUN_2025));

        assertThat(deductions.subscriptions()).isEqualTo(Money.of("500.00", BRL));
        assertThat(deductions.reservedBudgets()).isEqualTo(Money.of("0.00", BRL));
    }
}
