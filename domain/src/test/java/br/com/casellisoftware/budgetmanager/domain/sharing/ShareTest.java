package br.com.casellisoftware.budgetmanager.domain.sharing;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareTest {

    private static final Instant NOW = Instant.parse("2026-05-14T12:00:00Z");

    @Test
    void create_calculatesRatiosAndStartsActive() {
        Share share = Share.create(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.of("40.00"),
                List.of(
                        new Share.ShareQuotaAllocation("payer-1", Money.of("30.00")),
                        new Share.ShareQuotaAllocation("payer-2", Money.of("30.00"))
                ),
                "owner-1",
                NOW
        );

        assertThat(share.getStatus()).isEqualTo(ShareStatus.ACTIVE);
        assertThat(share.getOwnerRatio()).isEqualByComparingTo("0.40000000");
        assertThat(share.getQuotas()).hasSize(2);
        assertThat(share.isFullAssignment()).isFalse();
    }

    @Test
    void create_whenAmountsDoNotClose_throws() {
        assertThatThrownBy(() -> Share.create(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.of("40.00"),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("50.00"))),
                "owner-1",
                NOW
        )).isInstanceOf(ShareRatioMismatchException.class);
    }

    @Test
    void create_whenTotalAmountIsBelowTolerance_throws() {
        assertThatThrownBy(() -> Share.create(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("0.001"),
                Money.of("0.000"),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("0.01"))),
                "owner-1",
                NOW
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalAmount must be positive");
    }

    @Test
    void revert_isSymmetricAndIdempotentGuarded() {
        Share share = Share.create(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.of("0.00"),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("100.00"))),
                "owner-1",
                NOW
        );

        Share reverted = share.revert(NOW);
        assertThat(reverted.getStatus()).isEqualTo(ShareStatus.REVERTED);
        assertThat(reverted.getRevertedAt()).isEqualTo(Instant.parse("2026-05-14T12:00:00Z"));

        assertThatThrownBy(() -> reverted.revert(NOW))
                .isInstanceOf(ShareAlreadyRevertedException.class);
    }

    @Test
    void stopFrom_setsMonthAndKeepsActive() {
        Share share = subscriptionShare();

        Share stopped = share.stopFrom(YearMonth.of(2026, 6));

        assertThat(stopped.getStoppedFromMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(stopped.getStatus()).isEqualTo(ShareStatus.ACTIVE);
    }

    @Test
    void stopFrom_isEarliestWinsOnReStop() {
        Share stopped = subscriptionShare().stopFrom(YearMonth.of(2026, 6));

        Share later = stopped.stopFrom(YearMonth.of(2026, 8));
        assertThat(later.getStoppedFromMonth()).isEqualTo(YearMonth.of(2026, 6));
        assertThat(later).isSameAs(stopped);

        Share earlier = stopped.stopFrom(YearMonth.of(2026, 4));
        assertThat(earlier.getStoppedFromMonth()).isEqualTo(YearMonth.of(2026, 4));
    }

    @Test
    void stopFrom_rejectsExpenseSource() {
        Share expenseShare = Share.create(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                Money.of("100.00"),
                Money.of("40.00"),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("60.00"))),
                "owner-1",
                NOW
        );

        assertThatThrownBy(() -> expenseShare.stopFrom(YearMonth.of(2026, 6)))
                .isInstanceOf(ShareStopNotApplicableException.class);
    }

    @Test
    void stopFrom_rejectsRevertedShare() {
        Share reverted = subscriptionShare().revert(NOW);

        assertThatThrownBy(() -> reverted.stopFrom(YearMonth.of(2026, 6)))
                .isInstanceOf(ShareStopNotApplicableException.class);
    }

    @Test
    void isEffectiveFor_respectsStopBoundaryAndStatus() {
        Share active = subscriptionShare();
        assertThat(active.isEffectiveFor(YearMonth.of(2026, 6))).isTrue();

        Share stopped = active.stopFrom(YearMonth.of(2026, 6));
        assertThat(stopped.isEffectiveFor(YearMonth.of(2026, 5))).isTrue();   // before stop
        assertThat(stopped.isEffectiveFor(YearMonth.of(2026, 6))).isFalse();  // at stop
        assertThat(stopped.isEffectiveFor(YearMonth.of(2026, 7))).isFalse();  // after stop

        assertThat(subscriptionShare().revert(NOW).isEffectiveFor(YearMonth.of(2026, 5))).isFalse();
    }

    @Test
    void appendPaymentsPreservesStoppedFromMonth() {
        Share stopped = subscriptionShare().stopFrom(YearMonth.of(2026, 6));

        Share withPayment = stopped.appendPayments(null, java.util.Map.of("payer-1", "payment-1"));

        assertThat(withPayment.getStoppedFromMonth()).isEqualTo(YearMonth.of(2026, 6));
    }

    private static Share subscriptionShare() {
        return Share.create(
                "wallet-1",
                ShareSourceType.SUBSCRIPTION,
                "sub-1",
                Money.of("100.00"),
                Money.of("40.00"),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("60.00"))),
                "owner-1",
                NOW
        );
    }
}
