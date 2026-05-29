package br.com.casellisoftware.budgetmanager.persistence.payment.mappers;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentKind;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.payment.PaymentDocument;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPersistenceMapperTest {

    private static final Instant PAYMENT_DATE = Instant.parse("2026-04-10T12:00:00Z");
    private static final Currency USD = Currency.getInstance("USD");

    private final PaymentPersistenceMapper mapper = Mappers.getMapper(PaymentPersistenceMapper.class);

    @Test
    void toDocument_copiesAllFieldsAndCurrency() {
        Payment payment = Payment.create(
                Money.of(new BigDecimal("25.50"), USD),
                PAYMENT_DATE,
                "lunch",
                "expense-1",
                "wallet-1",
                "bullet-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        PaymentDocument document = mapper.toDocument(payment);

        assertThat(document.getId()).isEqualTo(payment.getId());
        assertThat(document.getAmount()).isEqualByComparingTo("25.50");
        assertThat(document.getCurrency()).isEqualTo("USD");
        assertThat(document.getPaymentDate()).isEqualTo(PAYMENT_DATE);
        assertThat(document.getDetails()).isEqualTo("lunch");
        assertThat(document.getExpenseId()).isEqualTo("expense-1");
        assertThat(document.getWalletId()).isEqualTo("wallet-1");
        assertThat(document.getBulletId()).isEqualTo("bullet-1");
        assertThat(document.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        assertThat(document.getKind()).isEqualTo(PaymentKind.NORMAL);
        assertThat(document.getPayerId()).isNull();
        assertThat(document.getShareId()).isNull();
        assertThat(document.isReversal()).isFalse();
        assertThat(document.getReversedPaymentId()).isNull();
    }

    @Test
    void toDomain_copiesAllFields() {
        PaymentDocument document = new PaymentDocument(
                "payment-1",
                new BigDecimal("10.00"),
                "BRL",
                PAYMENT_DATE,
                "coffee",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );
        document.setFlag(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);

        Payment payment = mapper.toDomain(document);

        assertThat(payment.getId()).isEqualTo("payment-1");
        assertThat(payment.getAmount()).isEqualTo(Money.of("10.00"));
        assertThat(payment.getPaymentDate()).isEqualTo(PAYMENT_DATE);
        assertThat(payment.getDetails()).isEqualTo("coffee");
        assertThat(payment.getExpenseId()).isEqualTo("expense-1");
        assertThat(payment.getWalletId()).isEqualTo("wallet-1");
        assertThat(payment.getBulletId()).isEqualTo("bullet-1");
        assertThat(payment.getFlag()).isEqualTo(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        assertThat(payment.getKind()).isEqualTo(PaymentKind.NORMAL);
        assertThat(payment.getPayerId()).isNull();
        assertThat(payment.getShareId()).isNull();
        assertThat(payment.isReversal()).isFalse();
        assertThat(payment.getReversedPaymentId()).isNull();
    }

    @Test
    void toDomain_whenCurrencyMissing_fallsBackToDefault() {
        PaymentDocument document = new PaymentDocument(
                "legacy-payment",
                new BigDecimal("10.00"),
                null,
                PAYMENT_DATE,
                "legacy",
                "expense-1",
                "wallet-1",
                null
        );

        Payment payment = mapper.toDomain(document);

        assertThat(payment.getAmount().currency()).isEqualTo(Money.DEFAULT_CURRENCY);
    }

    @Test
    void roundTrip_domainToDocumentToDomain_preservesState() {
        Payment original = Payment.createShared(
                Money.of("42.00"),
                PAYMENT_DATE,
                "dinner",
                "expense-1",
                "wallet-1",
                "bullet-1",
                "payer-1",
                "share-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        Payment roundTripped = mapper.toDomain(mapper.toDocument(original));

        assertThat(roundTripped)
                .usingRecursiveComparison()
                .isEqualTo(original);
    }

    @Test
    void toDomain_whenDocumentIsNull_returnsNull() {
        assertThat(mapper.toDomain(null)).isNull();
    }

    @Test
    void toDomain_mapsSharedAndReversalFields() {
        PaymentDocument document = new PaymentDocument(
                "payment-1",
                "owner-1",
                new BigDecimal("10.00"),
                "BRL",
                PAYMENT_DATE,
                "shared quota",
                "expense-1",
                "wallet-1",
                null,
                FlagEnum.NONE,
                PaymentKind.SHARED,
                "payer-1",
                "share-1",
                true,
                "payment-0"
        );

        Payment payment = mapper.toDomain(document);

        assertThat(payment.getKind()).isEqualTo(PaymentKind.SHARED);
        assertThat(payment.getPayerId()).isEqualTo("payer-1");
        assertThat(payment.getShareId()).isEqualTo("share-1");
        assertThat(payment.isReversal()).isTrue();
        assertThat(payment.getReversedPaymentId()).isEqualTo("payment-0");
    }
}
