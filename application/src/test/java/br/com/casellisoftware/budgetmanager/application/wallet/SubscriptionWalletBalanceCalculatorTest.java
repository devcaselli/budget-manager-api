package br.com.casellisoftware.budgetmanager.application.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionWalletBalanceCalculatorTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");
    private static final YearMonth MONTH = YearMonth.of(2026, 5);
    private final br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository shareRepository =
            org.mockito.Mockito.mock(br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository.class);

    @Test
    void subscriptionTotal_whenWalletNull_throwsNPE() {
        var subscription = Subscription.create("Netflix", BRL, Money.of(new BigDecimal("50"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        assertThatNullPointerException()
                .isThrownBy(() -> SubscriptionWalletBalanceCalculator.subscriptionTotal(null, List.of(subscription)))
                .withMessage("wallet must not be null");
    }

    @Test
    void subscriptionTotal_whenSubscriptionsNull_throwsNPE() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);

        assertThatNullPointerException()
                .isThrownBy(() -> SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, null))
                .withMessage("subscriptions must not be null");
    }

    @Test
    void subscriptionTotal_whenAllSameCurrency_sumsAmounts() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);

        var subscription1 = Subscription.create("Netflix", BRL, Money.of(new BigDecimal("50.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");
        var subscription2 = Subscription.create("Spotify", BRL, Money.of(new BigDecimal("30.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");
        var subscription3 = Subscription.create("Disney+", BRL, Money.of(new BigDecimal("20.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        var result = SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, List.of(subscription1, subscription2, subscription3));

        assertThat(result.amount()).isEqualTo(new BigDecimal("100.00"));
        assertThat(result.currency()).isEqualTo(BRL);
    }

    @Test
    void subscriptionTotal_whenSubscriptionCurrencyMismatch_throwsWalletCurrencyMismatchException() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);

        var usdSubscription = Subscription.create("US Service", USD, Money.of(new BigDecimal("100.00"), USD), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        assertThatThrownBy(() -> SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, List.of(usdSubscription)))
                .isInstanceOf(WalletCurrencyMismatchException.class)
                .hasMessageContaining("BRL")
                .hasMessageContaining("USD")
                .hasMessageContaining("walletId=" + wallet.getId())
                .hasMessageContaining("subscriptionId=" + usdSubscription.getId());
    }

    @Test
    void effectiveRemainingAmount_subtractsSubscriptionTotalFromWalletRemaining() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);

        var subscription1 = Subscription.create("Netflix", BRL, Money.of(new BigDecimal("50.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");
        var subscription2 = Subscription.create("Spotify", BRL, Money.of(new BigDecimal("30.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        var result = SubscriptionWalletBalanceCalculator.effectiveRemainingAmount(wallet, List.of(subscription1, subscription2));

        assertThat(result).isEqualTo(new BigDecimal("920.00"));
    }

    @Test
    void effectiveRemainingAmount_whenCurrencyMismatch_propagatesException() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);

        var usdSubscription = Subscription.create("US Service", USD, Money.of(new BigDecimal("100.00"), USD), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        assertThatThrownBy(() -> SubscriptionWalletBalanceCalculator.effectiveRemainingAmount(wallet, List.of(usdSubscription)))
                .isInstanceOf(WalletCurrencyMismatchException.class)
                .hasMessageContaining("BRL")
                .hasMessageContaining("USD")
                .hasMessageContaining("walletId=" + wallet.getId())
                .hasMessageContaining("subscriptionId=" + usdSubscription.getId());
    }

    @Test
    void subscriptionTotal_whenActiveShareExists_reservesOnlyOwnerPortion() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);
        var subscription = Subscription.create("Netflix", BRL, Money.of(new BigDecimal("200.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        br.com.casellisoftware.budgetmanager.domain.sharing.Share share =
                new br.com.casellisoftware.budgetmanager.domain.sharing.Share(
                        "share-1",
                        subscription.getOwnerId(),
                        wallet.getId(),
                        br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.SUBSCRIPTION,
                        subscription.getId(),
                        Money.of(new BigDecimal("200.00"), BRL),
                        Money.of(new BigDecimal("50.00"), BRL),
                        new BigDecimal("0.25000000"),
                        List.of(new br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota(
                                "payer-1", new BigDecimal("0.75000000"), List.of())),
                        br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus.ACTIVE,
                        List.of(),
                        java.time.Instant.parse("2026-05-01T00:00:00Z"),
                        null,
                        null
                );
        org.mockito.Mockito.when(shareRepository.findActiveBySourceIds(
                org.mockito.ArgumentMatchers.eq(br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType.SUBSCRIPTION),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(subscription.getOwnerId())
        )).thenReturn(java.util.Map.of(subscription.getId(), share));

        Money total = SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, List.of(subscription), shareRepository);

        assertThat(total).isEqualTo(Money.of(new BigDecimal("50.00"), BRL));
    }

    @Test
    void subscriptionTotal_whenNoActiveShare_keepsGrossSubscriptionAmount() {
        var wallet = Wallet.create("May Budget", Money.of(new BigDecimal("1000"), BRL), null, LocalDate.now(), false, MONTH, WalletState.PRODUCTION, null);
        var subscription = Subscription.create("Netflix", BRL, Money.of(new BigDecimal("200.00"), BRL), MONTH, SubscriptionState.PRODUCTION, null, "cc-test");

        org.mockito.Mockito.when(shareRepository.findActiveBySourceId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(java.util.Optional.empty());

        Money total = SubscriptionWalletBalanceCalculator.subscriptionTotal(wallet, List.of(subscription));

        assertThat(total).isEqualTo(Money.of(new BigDecimal("200.00"), BRL));
    }
}
