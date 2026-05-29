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
import java.time.Instant;
import java.time.LocalDate;
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

    private PayerAmountDueCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PayerAmountDueCalculator(shareRepository, installmentRepository);
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
                null
        );
    }
}
