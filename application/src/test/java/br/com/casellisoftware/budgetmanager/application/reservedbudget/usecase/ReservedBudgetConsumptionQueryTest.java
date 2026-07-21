package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Verifies consumed/remaining computation for a ReservedBudget given its links.
 *
 * <p>Consumed = sum of post-share effective amounts of links applicable in the target month
 * whose source item is active that month. Remaining = ceiling − consumed.</p>
 */
@ExtendWith(MockitoExtension.class)
class ReservedBudgetConsumptionQueryTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final String OWNER = "owner-1";
    private static final YearMonth MAY_2025 = YearMonth.of(2025, 5);
    private static final YearMonth JUN_2025 = YearMonth.of(2025, 6);

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private ShareRepository shareRepository;

    private ReservedBudgetConsumptionQuery query;

    @BeforeEach
    void setUp() {
        lenient().when(shareRepository.findActiveBySourceIds(any(), any(), anyString())).thenReturn(Map.of());
        query = new ReservedBudgetConsumptionQuery(
                subscriptionRepository, installmentRepository, shareRepository);
    }

    @Test
    void noLinks_consumedZero_remainingEqualsCeiling() {
        ReservedBudget rb = rb();

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, JUN_2025, OWNER);

        assertThat(result.consumed()).isEqualTo(Money.of("0.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void rbNotYetActiveInMonth_returnsZeroZero() {
        // RB starts 2026-07; querying current month 2026-06 (before start) must not throw.
        ReservedBudget rb = ReservedBudget.create(
                "RB", null, BRL, Money.of("2000.00", BRL), YearMonth.of(2026, 7), FlagEnum.NONE, OWNER);

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, YearMonth.of(2026, 6), OWNER);

        assertThat(result.consumed()).isEqualTo(Money.of("0.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("0.00", BRL));
    }

    @Test
    void singleSubscriptionLink_applicable_consumesItsAmount() {
        Subscription netflix = subscription("Netflix");
        ReservedBudget rb = rb().addLink(
                new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025));

        when(subscriptionRepository.findAllByIds(List.of(netflix.getId()), OWNER))
                .thenReturn(Map.of(netflix.getId(), netflix));

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, JUN_2025, OWNER);

        assertThat(result.consumed()).isEqualTo(Money.of("500.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("1500.00", BRL));
    }

    @Test
    void linkBeforeFromMonth_notApplicable_consumesZero() {
        Subscription netflix = subscription("Netflix");
        // link applicable only from June; we query May → not applicable
        ReservedBudget rb = rb().addLink(
                new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025));

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, MAY_2025, OWNER);

        assertThat(result.consumed()).isEqualTo(Money.of("0.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void subscriptionWithShare_consumesPostShareAmount() {
        Subscription netflix = subscription("Netflix");
        // ownerShare 50 of total 100 → ownerRatio 0.50
        Share half = Share.create(
                "wallet-jun",
                ShareSourceType.SUBSCRIPTION,
                netflix.getId(),
                Money.of("100.00", BRL),
                Money.of("50.00", BRL),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("50.00", BRL))),
                OWNER,
                Instant.parse("2025-01-01T00:00:00Z"));
        ReservedBudget rb = rb().addLink(
                new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025));

        when(subscriptionRepository.findAllByIds(List.of(netflix.getId()), OWNER))
                .thenReturn(Map.of(netflix.getId(), netflix));
        when(shareRepository.findActiveBySourceIds(
                eq(ShareSourceType.SUBSCRIPTION), eq(List.of(netflix.getId())), eq(OWNER)))
                .thenReturn(Map.of(netflix.getId(), half));

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, JUN_2025, OWNER);

        // 500 * 0.50 = 250
        assertThat(result.consumed()).isEqualTo(Money.of("250.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("1750.00", BRL));
    }

    @Test
    void multipleLinks_sumsApplicableOnes() {
        Subscription netflix = subscription("Netflix");
        Subscription spotify = subscription("Spotify");
        ReservedBudget rb = rb()
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, netflix.getId(), JUN_2025))
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, spotify.getId(), JUN_2025));

        when(subscriptionRepository.findAllByIds(any(), eq(OWNER)))
                .thenReturn(Map.of(netflix.getId(), netflix, spotify.getId(), spotify));

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, JUN_2025, OWNER);

        // 500 + 500 = 1000
        assertThat(result.consumed()).isEqualTo(Money.of("1000.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("1000.00", BRL));
    }

    @Test
    void installmentLink_active_consumesInstallmentValue() {
        // notebook source month = May; from-source installments charge from the month after,
        // so it is active in June. Link applicable from May → applicable in June.
        Installment notebook = installment("Notebook", MAY_2025);
        ReservedBudget rb = rb().addLink(
                new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, notebook.getId(), MAY_2025));

        when(installmentRepository.findAllByIds(List.of(notebook.getId()), OWNER))
                .thenReturn(Map.of(notebook.getId(), notebook));

        ReservedBudgetConsumptionQuery.ReservedBudgetConsumption result =
                query.consume(rb, JUN_2025, OWNER);

        // installmentValue = 1000.00
        assertThat(result.consumed()).isEqualTo(Money.of("1000.00", BRL));
        assertThat(result.remaining()).isEqualTo(Money.of("1000.00", BRL));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private static ReservedBudget rb() {
        return ReservedBudget.create(
                "RB", null, BRL, Money.of("2000.00", BRL), YearMonth.of(2025, 1), FlagEnum.NONE, OWNER);
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
}
