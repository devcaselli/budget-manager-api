package br.com.casellisoftware.budgetmanager.domain.payment;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void patch_onlyUpdatesPatchableFields() {
        Payment payment = Payment.rebuild(
                "payment-1",
                Money.of("25.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "first",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        Payment patched = payment.patch(PaymentPatch.empty()
                .withAmount(Money.of("30.00"))
                .withDetails("updated"));

        assertThat(patched.getId()).isEqualTo("payment-1");
        assertThat(patched.getAmount()).isEqualTo(Money.of("30.00"));
        assertThat(patched.getDetails()).isEqualTo("updated");
        assertThat(patched.getPaymentDate()).isEqualTo(Instant.parse("2026-04-21T10:00:00Z"));
        assertThat(patched.getExpenseId()).isEqualTo("expense-1");
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
        assertThat(patched.getBulletId()).isEqualTo("bullet-1");
    }

    @Test
    void patch_whenEmpty_returnsSameInstance() {
        Payment payment = Payment.create(
                Money.of("25.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "first",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        assertThat(payment.patch(PaymentPatch.empty())).isSameAs(payment);
    }

    @Test
    void patch_rejectsNullPatch() {
        Payment payment = Payment.create(
                Money.of("25.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "first",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        assertThatThrownBy(() -> payment.patch(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("patch");
    }

    @Test
    void createShared_setsSharedMetadata() {
        Payment payment = Payment.createShared(
                Money.of("10.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "shared quota",
                "expense-1",
                "wallet-1",
                null,
                "payer-1",
                "share-1",
                br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE
        );

        assertThat(payment.getKind()).isEqualTo(PaymentKind.SHARED);
        assertThat(payment.getPayerId()).isEqualTo("payer-1");
        assertThat(payment.getShareId()).isEqualTo("share-1");
        assertThat(payment.isReversal()).isFalse();
        assertThat(payment.getReversedPaymentId()).isNull();
    }

    @Test
    void createReversal_linksOriginalPayment() {
        Payment original = Payment.createShared(
                Money.of("10.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "shared quota",
                "expense-1",
                "wallet-1",
                null,
                "payer-1",
                "share-1",
                br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE
        );

        Payment reversal = Payment.createReversal(
                original,
                Instant.parse("2026-04-22T10:00:00Z"),
                "reverted by share share-1",
                "owner-1"
        );

        assertThat(reversal.isReversal()).isTrue();
        assertThat(reversal.getReversedPaymentId()).isEqualTo(original.getId());
        assertThat(reversal.getKind()).isEqualTo(PaymentKind.SHARED);
        assertThat(reversal.getShareId()).isEqualTo("share-1");
        assertThat(reversal.getPayerId()).isEqualTo("payer-1");
    }

    @Test
    void sharedPaymentWithoutShareId_isRejected() {
        assertThatThrownBy(() -> new Payment(
                "payment-1",
                "owner-1",
                Money.of("10.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "shared quota",
                "expense-1",
                "wallet-1",
                null,
                br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE,
                PaymentKind.SHARED,
                "payer-1",
                null,
                false,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("shareId");
    }
}
