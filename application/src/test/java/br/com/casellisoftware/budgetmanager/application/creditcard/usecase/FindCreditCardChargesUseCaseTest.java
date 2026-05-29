package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardChargesOutput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardResult;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindCreditCardChargesUseCaseTest {

    private static final String CARD_ID = "card-1";
    private static final String OWNER_ID = "owner-1";
    private static final YearMonth MONTH = YearMonth.of(2026, 5);

    @Mock private CreditCardRepository creditCardRepository;
    @Mock private ExpenseRepository expenseRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private InstallmentRepository installmentRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private ShareRepository shareRepository;

    private FindCreditCardChargesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindCreditCardChargesUseCase(
                creditCardRepository, expenseRepository, walletRepository,
                installmentRepository, subscriptionRepository, shareRepository);
        when(creditCardRepository.findById(CARD_ID, OWNER_ID))
                .thenReturn(Optional.of(CreditCard.create("Nubank", OWNER_ID)));
    }

    @Test
    void execute_installmentWithShare_appliesOwnerRatioToEffectiveInstallmentValue() {
        Installment installment = Installment.rebuild(
                "inst-1",
                "Laptop",
                null,
                Money.of("2000.00"),
                Money.of("200.00"),
                10,
                LocalDate.of(2026, 1, 10),
                YearMonth.of(2026, 10),
                CARD_ID,
                null,
                "wallet-1",
                YearMonth.of(2026, 1),
                false,
                null,
                FlagEnum.NONE,
                OWNER_ID
        );

        BigDecimal ownerRatio = new BigDecimal("0.50000000");
        Share share = Share.create(
                "wallet-1",
                ShareSourceType.INSTALLMENT,
                "inst-1",
                Money.of(new BigDecimal("2000.00"), Currency.getInstance("BRL")),
                Money.of(new BigDecimal("1000.00"), Currency.getInstance("BRL")),
                List.of(new Share.ShareQuotaAllocation(
                        "payer-1",
                        Money.of(new BigDecimal("1000.00"), Currency.getInstance("BRL")))),
                OWNER_ID,
                java.time.Instant.now()
        );

        when(walletRepository.findIdsByEffectiveMonth(MONTH, OWNER_ID)).thenReturn(List.of("wallet-1"));
        when(expenseRepository.findByCreditCardId(anyString(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new ExpenseByCreditCardResult(
                        new PageResult<>(List.of(), 0, 1000, 0, 0), BigDecimal.ZERO));
        when(installmentRepository.findActiveAffecting(MONTH, OWNER_ID)).thenReturn(List.of(installment));
        when(shareRepository.findActiveBySourceIds(eq(ShareSourceType.INSTALLMENT), any(), eq(OWNER_ID)))
                .thenReturn(Map.of("inst-1", share));
        when(subscriptionRepository.findActiveForByOwnerId(MONTH, OWNER_ID)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(shareRepository.findActiveBySourceIds(eq(ShareSourceType.SUBSCRIPTION), any(), eq(OWNER_ID)))
                .thenReturn(Map.of());

        CreditCardChargesOutput output = useCase.execute(CARD_ID, MONTH, OWNER_ID);

        assertThat(output.installments()).hasSize(1);
        assertThat(output.installments().get(0).shared()).isTrue();
        assertThat(output.installments().get(0).ownerRatio()).isEqualByComparingTo(ownerRatio);
        BigDecimal expectedEffective = new BigDecimal("200.00").multiply(ownerRatio)
                .setScale(2, RoundingMode.HALF_EVEN);
        assertThat(output.installments().get(0).effectiveInstallmentValue())
                .isEqualByComparingTo(expectedEffective);
    }

    @Test
    void execute_subscriptionWithShare_appliesOwnerRatioToAmount() {
        Subscription subscription = Subscription.rebuild(
                "sub-1",
                "Netflix",
                Currency.getInstance("BRL"),
                YearMonth.of(2025, 1),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2025, 1), Money.of("50.00"))),
                FlagEnum.NONE,
                OWNER_ID,
                CARD_ID
        );

        Share share = Share.create(
                "wallet-1",
                ShareSourceType.SUBSCRIPTION,
                "sub-1",
                Money.of(new BigDecimal("50.00"), Currency.getInstance("BRL")),
                Money.of(new BigDecimal("25.00"), Currency.getInstance("BRL")),
                List.of(new Share.ShareQuotaAllocation(
                        "payer-1",
                        Money.of(new BigDecimal("25.00"), Currency.getInstance("BRL")))),
                OWNER_ID,
                java.time.Instant.now()
        );

        when(walletRepository.findIdsByEffectiveMonth(MONTH, OWNER_ID)).thenReturn(List.of("wallet-1"));
        when(expenseRepository.findByCreditCardId(anyString(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new ExpenseByCreditCardResult(
                        new PageResult<>(List.of(), 0, 1000, 0, 0), BigDecimal.ZERO));
        when(installmentRepository.findActiveAffecting(MONTH, OWNER_ID)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(shareRepository.findActiveBySourceIds(eq(ShareSourceType.INSTALLMENT), any(), eq(OWNER_ID)))
                .thenReturn(Map.of());
        when(subscriptionRepository.findActiveForByOwnerId(MONTH, OWNER_ID)).thenReturn(List.of(subscription));
        when(shareRepository.findActiveBySourceIds(eq(ShareSourceType.SUBSCRIPTION), any(), eq(OWNER_ID)))
                .thenReturn(Map.of("sub-1", share));

        CreditCardChargesOutput output = useCase.execute(CARD_ID, MONTH, OWNER_ID);

        assertThat(output.subscriptions()).hasSize(1);
        BigDecimal expectedAmount = new BigDecimal("50.00")
                .multiply(new BigDecimal("0.50000000"))
                .setScale(2, RoundingMode.HALF_EVEN);
        assertThat(output.subscriptions().get(0).amount()).isEqualByComparingTo(expectedAmount);
    }

    @Test
    void execute_noSharesInBatch_returnsFullAmount() {
        Installment installment = Installment.rebuild(
                "inst-2",
                "Phone",
                null,
                Money.of("600.00"),
                Money.of("100.00"),
                6,
                LocalDate.of(2026, 1, 5),
                YearMonth.of(2026, 6),
                CARD_ID,
                null,
                "wallet-1",
                YearMonth.of(2026, 1),
                false,
                null,
                FlagEnum.NONE,
                OWNER_ID
        );

        when(walletRepository.findIdsByEffectiveMonth(MONTH, OWNER_ID)).thenReturn(List.of("wallet-1"));
        when(expenseRepository.findByCreditCardId(anyString(), any(), anyInt(), anyInt(), anyString()))
                .thenReturn(new ExpenseByCreditCardResult(
                        new PageResult<>(List.of(), 0, 1000, 0, 0), BigDecimal.ZERO));
        when(installmentRepository.findActiveAffecting(MONTH, OWNER_ID)).thenReturn(List.of(installment));
        org.mockito.Mockito.lenient().when(shareRepository.findActiveBySourceIds(eq(ShareSourceType.INSTALLMENT), any(), eq(OWNER_ID)))
                .thenReturn(Map.of());
        when(subscriptionRepository.findActiveForByOwnerId(MONTH, OWNER_ID)).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(shareRepository.findActiveBySourceIds(eq(ShareSourceType.SUBSCRIPTION), any(), eq(OWNER_ID)))
                .thenReturn(Map.of());

        CreditCardChargesOutput output = useCase.execute(CARD_ID, MONTH, OWNER_ID);

        assertThat(output.installments()).hasSize(1);
        assertThat(output.installments().get(0).shared()).isFalse();
        assertThat(output.installments().get(0).effectiveInstallmentValue())
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }
}
