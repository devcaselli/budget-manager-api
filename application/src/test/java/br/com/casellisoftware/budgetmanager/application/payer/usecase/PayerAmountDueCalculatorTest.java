package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareQuota;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayerAmountDueCalculatorTest {

    private static final String OWNER = "owner-1";
    private static final String PAYER_ID = "payer-1";

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    // Evaluation month is June 2026 (fixed). A share stopped from June or earlier
    // contributes no forward monthly; a share stopped from July still counts.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-15T00:00:00Z"), ZoneOffset.UTC);

    private PayerAmountDueCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PayerAmountDueCalculator(shareRepository, installmentRepository, CLOCK);
    }

    @Test
    void calculate_noActiveShares_returnsZero() {
        when(shareRepository.findActiveByPayerId(PAYER_ID, OWNER)).thenReturn(List.of());

        PayerAmountDue due = calculator.calculate(payer(), OWNER);

        assertThat(due.monthly()).isEqualTo(Money.zero());
        assertThat(due.journey()).isEqualTo(Money.zero());
    }

    @Test
    void calculate_expenseShare_monthlyEqualsJourney() {
        Share share = expenseShare(Money.of("100.00"), new BigDecimal("0.5"));
        when(shareRepository.findActiveByPayerId(PAYER_ID, OWNER)).thenReturn(List.of(share));

        PayerAmountDue due = calculator.calculate(payer(), OWNER);

        assertThat(due.monthly().amount()).isEqualByComparingTo("50.00");
        assertThat(due.journey().amount()).isEqualByComparingTo("50.00");
    }

    private Payer payer() {
        return new Payer(
                PAYER_ID,
                OWNER,
                "Payer",
                PayerType.STANDING,
                null,
                LocalDate.of(2026, 5, 10),
                false);
    }

    private Share expenseShare(Money total, BigDecimal payerRatio) {
        BigDecimal ownerRatio = BigDecimal.ONE.subtract(payerRatio);
        Money ownerShare = Money.of(total.amount().multiply(ownerRatio), total.currency());
        return new Share(
                "share-1",
                OWNER,
                "wallet-1",
                ShareSourceType.EXPENSE,
                "source-1",
                total,
                ownerShare,
                ownerRatio,
                List.of(new ShareQuota(PAYER_ID, payerRatio, List.of())),
                ShareStatus.ACTIVE,
                List.of(),
                Instant.parse("2026-05-01T00:00:00Z"),
                null,
                null
        );
    }

    private Share subscriptionShare(Money total, BigDecimal payerRatio, YearMonth stoppedFromMonth) {
        BigDecimal ownerRatio = BigDecimal.ONE.subtract(payerRatio);
        Money ownerShare = Money.of(total.amount().multiply(ownerRatio), total.currency());
        return new Share(
                "share-sub",
                OWNER,
                "wallet-1",
                ShareSourceType.SUBSCRIPTION,
                "source-sub",
                total,
                ownerShare,
                ownerRatio,
                List.of(new ShareQuota(PAYER_ID, payerRatio, List.of())),
                ShareStatus.ACTIVE,
                List.of(),
                Instant.parse("2026-05-01T00:00:00Z"),
                null,
                stoppedFromMonth
        );
    }

    @Test
    void calculate_subscriptionShareStoppedBeforeEvalMonth_excludesMonthlyKeepsJourney() {
        // Stopped from June; eval month is June -> not effective -> no forward monthly,
        // but the accumulated journey still stands (approximation).
        Share stopped = subscriptionShare(Money.of("100.00"), new BigDecimal("0.5"), YearMonth.of(2026, 6));
        when(shareRepository.findActiveByPayerId(PAYER_ID, OWNER)).thenReturn(List.of(stopped));

        PayerAmountDue due = calculator.calculate(payer(), OWNER);

        assertThat(due.monthly().amount()).isEqualByComparingTo("0.00");
        assertThat(due.journey().amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void calculate_subscriptionShareStoppedAfterEvalMonth_stillCountsMonthly() {
        // Stopped from July; eval month is June -> still effective -> monthly counts.
        Share future = subscriptionShare(Money.of("100.00"), new BigDecimal("0.5"), YearMonth.of(2026, 7));
        when(shareRepository.findActiveByPayerId(PAYER_ID, OWNER)).thenReturn(List.of(future));

        PayerAmountDue due = calculator.calculate(payer(), OWNER);

        assertThat(due.monthly().amount()).isEqualByComparingTo("50.00");
        assertThat(due.journey().amount()).isEqualByComparingTo("50.00");
    }
}
