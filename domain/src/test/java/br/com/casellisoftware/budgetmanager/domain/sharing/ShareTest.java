package br.com.casellisoftware.budgetmanager.domain.sharing;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
}
