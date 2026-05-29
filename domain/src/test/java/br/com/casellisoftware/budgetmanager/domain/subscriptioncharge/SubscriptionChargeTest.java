package br.com.casellisoftware.budgetmanager.domain.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionChargeTest {

    @Test
    void create_initializesRemainingWithAmountSnapshot() {
        SubscriptionCharge charge = SubscriptionCharge.create(
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("55.90"),
                FlagEnum.NONE
        );

        assertThat(charge.getId()).isNotBlank();
        assertThat(charge.getSubscriptionId()).isEqualTo("subscription-1");
        assertThat(charge.getWalletId()).isEqualTo("wallet-1");
        assertThat(charge.getMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(charge.getAmount()).isEqualTo(Money.of("55.90"));
        assertThat(charge.getRemaining()).isEqualTo(Money.of("55.90"));
    }

    @Test
    void debit_returnsChargeWithReducedRemaining() {
        SubscriptionCharge charge = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("80.00"),
                FlagEnum.NONE
        );

        SubscriptionCharge debited = charge.debit(Money.of("30.00"));

        assertThat(debited.getRemaining()).isEqualTo(Money.of("50.00"));
        assertThat(debited.getAmount()).isEqualTo(Money.of("100.00"));
        assertThat(debited.getId()).isEqualTo("charge-1");
        assertThat(charge.getRemaining()).isEqualTo(Money.of("80.00"));
    }

    @Test
    void pay_debitsPaymentAmount() {
        SubscriptionCharge charge = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("80.00"),
                FlagEnum.NONE
        );
        Payment payment = Payment.rebuild(
                "payment-1",
                Money.of("25.00"),
                Instant.parse("2026-05-10T10:00:00Z"),
                "subscription payment",
                "expense-1",
                "wallet-1",
                null
        );

        SubscriptionCharge paid = charge.pay(payment);

        assertThat(paid.getRemaining()).isEqualTo(Money.of("55.00"));
    }

    @Test
    void consumed_returnsAmountMinusRemaining() {
        SubscriptionCharge charge = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("70.00"),
                FlagEnum.NONE
        );

        assertThat(charge.consumed()).isEqualTo(Money.of("30.00"));
    }

    @Test
    void consumed_whenRemainingEqualsAmount_returnsZero() {
        SubscriptionCharge charge = SubscriptionCharge.create(
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                FlagEnum.NONE
        );

        assertThat(charge.consumed()).isEqualTo(Money.zero());
    }

    @Test
    void create_rejectsInvalidRequiredFields() {
        assertThatThrownBy(() -> SubscriptionCharge.create(null, "wallet-1", YearMonth.of(2026, 5), Money.of("10.00"), FlagEnum.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("subscriptionId must not be null");
        assertThatThrownBy(() -> SubscriptionCharge.create(" ", "wallet-1", YearMonth.of(2026, 5), Money.of("10.00"), FlagEnum.NONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subscriptionId must not be blank");
        assertThatThrownBy(() -> SubscriptionCharge.create("subscription-1", null, YearMonth.of(2026, 5), Money.of("10.00"), FlagEnum.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("walletId must not be null");
        assertThatThrownBy(() -> SubscriptionCharge.create("subscription-1", " ", YearMonth.of(2026, 5), Money.of("10.00"), FlagEnum.NONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId must not be blank");
        assertThatThrownBy(() -> SubscriptionCharge.create("subscription-1", "wallet-1", null, Money.of("10.00"), FlagEnum.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("month");
        assertThatThrownBy(() -> SubscriptionCharge.create("subscription-1", "wallet-1", YearMonth.of(2026, 5), null, FlagEnum.NONE))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> SubscriptionCharge.create("subscription-1", "wallet-1", YearMonth.of(2026, 5), Money.zero(), FlagEnum.NONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void pay_rejectsNullPayment() {
        SubscriptionCharge charge = SubscriptionCharge.create(
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("55.90"),
                FlagEnum.NONE
        );

        assertThatThrownBy(() -> charge.pay(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payment");
    }

    @Test
    void debit_rejectsAmountGreaterThanRemaining() {
        SubscriptionCharge charge = SubscriptionCharge.create(
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("55.90"),
                FlagEnum.NONE
        );

        assertThatThrownBy(() -> charge.debit(Money.of("55.91")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("debit amount exceeds remaining");
    }

    @Test
    void credit_returnsChargeWithIncreasedRemaining() {
        SubscriptionCharge charge = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("70.00"),
                FlagEnum.NONE
        );

        SubscriptionCharge credited = charge.credit(Money.of("20.00"));

        assertThat(credited.getRemaining()).isEqualTo(Money.of("90.00"));
        assertThat(credited.getAmount()).isEqualTo(Money.of("100.00"));
        assertThat(charge.getRemaining()).isEqualTo(Money.of("70.00"));
    }

    @Test
    void credit_rejectsAmountThatOverflowsOriginalAmount() {
        SubscriptionCharge charge = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("80.00"),
                FlagEnum.NONE
        );

        assertThatThrownBy(() -> charge.credit(Money.of("30.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("credit overflows charge amount");
    }

    @Test
    void credit_isSymmetricInverseOfDebit() {
        SubscriptionCharge charge = SubscriptionCharge.create(
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                FlagEnum.NONE
        );

        SubscriptionCharge roundTripped = charge.debit(Money.of("40.00")).credit(Money.of("40.00"));

        assertThat(roundTripped.getRemaining()).isEqualTo(charge.getRemaining());
    }

    @Test
    void equalsAndHashCode_areBasedOnId() {
        SubscriptionCharge first = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("70.00"),
                FlagEnum.NONE
        );
        SubscriptionCharge second = SubscriptionCharge.rebuild(
                "charge-1",
                "subscription-2",
                "wallet-2",
                YearMonth.of(2026, 6),
                Money.of("200.00"),
                Money.of("200.00"),
                FlagEnum.NONE
        );
        SubscriptionCharge other = SubscriptionCharge.rebuild(
                "charge-2",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                Money.of("100.00"),
                Money.of("70.00"),
                FlagEnum.NONE
        );

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(other);
        assertThat(first).isNotEqualTo("charge-1");
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
