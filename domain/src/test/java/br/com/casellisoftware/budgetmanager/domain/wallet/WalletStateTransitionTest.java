package br.com.casellisoftware.budgetmanager.domain.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.IllegalWalletStateTransitionException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletStateTransitionTest {

    private Wallet wallet(WalletState state, boolean closed, LocalDate closedDate) {
        return new Wallet("w1", "monthly",
                Money.of("100.00"), Money.of("100.00"),
                LocalDate.of(2026, 5, 1), closedDate, closed,
                YearMonth.of(2026, 5), state, FlagEnum.NONE);
    }

    @Test
    void productionToReview_requiresClosed() {
        Wallet open = wallet(WalletState.PRODUCTION, false, null);
        assertThatThrownBy(() -> open.transitionTo(WalletState.REVIEW))
                .isInstanceOf(IllegalWalletStateTransitionException.class);
    }

    @Test
    void productionToReview_whenClosed_succeeds() {
        Wallet closed = wallet(WalletState.PRODUCTION, true, null);
        Wallet review = closed.transitionTo(WalletState.REVIEW);
        assertThat(review.getState()).isEqualTo(WalletState.REVIEW);
    }

    @Test
    void productionToPreview_forbidden() {
        Wallet w = wallet(WalletState.PRODUCTION, false, null);
        assertThatThrownBy(() -> w.transitionTo(WalletState.PREVIEW))
                .isInstanceOf(IllegalWalletStateTransitionException.class);
    }

    @Test
    void previewToProduction_allowed() {
        Wallet preview = wallet(WalletState.PREVIEW, false, null);
        Wallet prod = preview.transitionTo(WalletState.PRODUCTION);
        assertThat(prod.getState()).isEqualTo(WalletState.PRODUCTION);
    }

    @Test
    void previewToReview_forbidden() {
        Wallet preview = wallet(WalletState.PREVIEW, true, null);
        assertThatThrownBy(() -> preview.transitionTo(WalletState.REVIEW))
                .isInstanceOf(IllegalWalletStateTransitionException.class);
    }

    @Test
    void reviewIsTerminal() {
        Wallet review = wallet(WalletState.REVIEW, true, null);
        assertThatThrownBy(() -> review.transitionTo(WalletState.PRODUCTION))
                .isInstanceOf(IllegalWalletStateTransitionException.class);
    }

    @Test
    void sameState_returnsSameInstance() {
        Wallet w = wallet(WalletState.PRODUCTION, false, null);
        assertThat(w.transitionTo(WalletState.PRODUCTION)).isSameAs(w);
    }

    @Test
    void isClosed_byFlag() {
        Wallet w = wallet(WalletState.PRODUCTION, true, null);
        assertThat(w.isClosed(LocalDate.of(2026, 5, 1))).isTrue();
    }

    @Test
    void isClosed_byClosedDateInPast() {
        Wallet w = wallet(WalletState.PRODUCTION, false, LocalDate.of(2026, 4, 30));
        assertThat(w.isClosed(LocalDate.of(2026, 5, 1))).isTrue();
    }

    @Test
    void isClosed_falseWhenOpenAndNoClosedDate() {
        Wallet w = wallet(WalletState.PRODUCTION, false, null);
        assertThat(w.isClosed(LocalDate.of(2026, 5, 1))).isFalse();
    }

    @Test
    void isClosed_falseWhenClosedDateInFuture() {
        Wallet w = wallet(WalletState.PRODUCTION, false, LocalDate.of(2026, 6, 30));
        assertThat(w.isClosed(LocalDate.of(2026, 5, 1))).isFalse();
    }
}
