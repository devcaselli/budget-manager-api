package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.NoSubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private ShareRepository shareRepository;

    private SaveWalletUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(installmentRepository.findActiveAffecting(any(YearMonth.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        useCase = new SaveWalletUseCase(
                walletRepository,
                subscriptionRepository,
                installmentRepository,
                shareRepository,
                java.time.Clock.systemDefaultZone()
        );
    }

    @Test
    void execute_createsWalletPersistsAndReturnsOutput() {
        WalletInput input = new WalletInput(
                "May wallet",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy")).thenReturn(List.of());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletOutput output = useCase.execute(input);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getDescription()).isEqualTo("May wallet");
        assertThat(saved.getBudget()).isEqualTo(Money.of("3000.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("3000.00"));
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(saved.getClosed()).isFalse();

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.description()).isEqualTo("May wallet");
        assertThat(output.budget()).isEqualByComparingTo("3000.00");
        assertThat(output.remaining()).isEqualByComparingTo("3000.00");
    }

    @Test
    void execute_whenRepositoryFails_propagates() {
        WalletInput input = new WalletInput(
                "May wallet",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        RuntimeException boom = new RuntimeException("mongo down");
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy")).thenReturn(List.of());
        when(walletRepository.save(any(Wallet.class))).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);
    }

    @Test
    void execute_computesSubscriptionImpactWithoutPersistingDebit() {
        WalletInput input = new WalletInput(
                "May wallet",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(subscription("subscription-1", "200.00")));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletOutput output = useCase.execute(input);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertThat(captor.getValue().getRemaining()).isEqualTo(Money.of("3000.00"));
        assertThat(output.remaining()).isEqualByComparingTo("2800.00");
    }

    @Test
    void execute_whenSubscriptionsExceedBudget_doesNotPersistWalletAndPropagates() {
        WalletInput input = new WalletInput(
                "May wallet",
                new BigDecimal("100.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(subscription("subscription-1", "150.00")));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException.class);

        verify(walletRepository, never()).save(any(Wallet.class));
    }

    @Test
    void execute_withNoSubscriptionRepository_doesNotApplySubscriptionCharges() {
        useCase = new SaveWalletUseCase(
                walletRepository,
                NoSubscriptionRepository.INSTANCE,
                installmentRepository,
                shareRepository,
                java.time.Clock.systemDefaultZone()
        );
        WalletInput input = new WalletInput(
                "May wallet",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletOutput output = useCase.execute(input);

        assertThat(output.remaining()).isEqualByComparingTo("3000.00");
    }

    @Test
    void execute_computesInstallmentImpactWithoutPersistingDebit() {
        WalletInput input = new WalletInput(
                "June wallet",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 6, 1),
                null,
                false,
                null,
                null
        );
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 6), SubscriptionState.PRODUCTION, "legacy")).thenReturn(List.of());
        when(installmentRepository.findActiveAffecting(YearMonth.of(2026, 6), "legacy"))
                .thenReturn(List.of(installment("1000.00", YearMonth.of(2026, 5))));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletOutput output = useCase.execute(input);

        assertThat(output.remaining()).isEqualByComparingTo("2000.00");
    }

    private static Subscription subscription(String id, String amount) {
        return Subscription.rebuild(
                id,
                "subscription",
                Currency.getInstance("BRL"),
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of(amount))),
                FlagEnum.NONE
        );
    }

    private static Installment installment(String amount, YearMonth sourceMonth) {
        return Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00"), Currency.getInstance("BRL")),
                Money.of(new BigDecimal(amount), Currency.getInstance("BRL")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "wallet-source",
                sourceMonth,
                FlagEnum.NONE
        );
    }
}
