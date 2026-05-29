package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

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
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllWalletsUseCaseTest {

    private static final YearMonth MAY_2026 = YearMonth.of(2026, 5);

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private ShareRepository shareRepository;

    private FindAllWalletsUseCase useCase;
    private FindAllWalletsUseCase useCaseWithoutSubs;

    @BeforeEach
    void setUp() {
        lenient().when(installmentRepository.findActiveAffecting(any(YearMonth.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        lenient().when(installmentRepository.findActiveAffectingAny(any(Collection.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        useCaseWithoutSubs = new FindAllWalletsUseCase(
                walletRepository,
                NoSubscriptionRepository.INSTANCE,
                installmentRepository,
                shareRepository
        );
        useCase = new FindAllWalletsUseCase(walletRepository, subscriptionRepository, installmentRepository, shareRepository);
    }

    @Test
    void execute_returnsMappedWallets() {
        Wallet wallet = productionWallet("wallet-1", "Main", "3000.00", "1250.00");
        when(walletRepository.findAll("owner-1")).thenReturn(List.of(wallet));

        List<WalletOutput> result = useCaseWithoutSubs.execute("owner-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo("wallet-1");
        assertThat(result.getFirst().description()).isEqualTo("Main");
        assertThat(result.getFirst().budget()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.getFirst().remaining()).isEqualByComparingTo(new BigDecimal("1250.00"));
        assertThat(result.getFirst().startDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(result.getFirst().closedDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.getFirst().isClosed()).isFalse();
    }

    @Test
    void execute_whenNoWallets_returnsEmptyList() {
        when(walletRepository.findAll("owner-1")).thenReturn(List.of());

        assertThat(useCaseWithoutSubs.execute("owner-1")).isEmpty();
    }

    @Test
    void execute_withSubscriptions_deductsSubscriptionTotalFromRemaining() {
        Wallet wallet = productionWallet("wallet-1", "Main", "1000.00", "1000.00");
        Subscription subscription = subscription("sub-1", "200.00");
        when(walletRepository.findAll("owner-1")).thenReturn(List.of(wallet));
        when(subscriptionRepository.findActiveFor(MAY_2026, SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(subscription));

        List<WalletOutput> result = useCase.execute("owner-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().remaining()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    void execute_twoWalletsSameMonth_queriesSubscriptionRepositoryOnce() {
        Wallet wallet1 = productionWallet("wallet-1", "Alpha", "1000.00", "1000.00");
        Wallet wallet2 = productionWallet("wallet-2", "Beta", "2000.00", "2000.00");
        when(walletRepository.findAll("owner-1")).thenReturn(List.of(wallet1, wallet2));
        when(subscriptionRepository.findActiveFor(MAY_2026, SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of());

        useCase.execute("owner-1");

        verify(subscriptionRepository, times(1)).findActiveFor(MAY_2026, SubscriptionState.PRODUCTION, "legacy");
    }

    @Test
    void execute_twoWalletsDifferentMonths_queriesSubscriptionRepositoryTwice() {
        Wallet wallet1 = productionWallet("wallet-1", "Alpha", "1000.00", "1000.00");
        Wallet wallet2 = new Wallet(
                "wallet-2", "Beta",
                Money.of("2000.00"), Money.of("2000.00"),
                LocalDate.of(2026, 4, 1), null, false,
                YearMonth.of(2026, 4),
                WalletState.PRODUCTION, FlagEnum.NONE
        );
        when(walletRepository.findAll("owner-1")).thenReturn(List.of(wallet1, wallet2));
        when(subscriptionRepository.findActiveFor(MAY_2026, SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of());
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 4), SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of());

        useCase.execute("owner-1");

        verify(subscriptionRepository, times(1)).findActiveFor(MAY_2026, SubscriptionState.PRODUCTION, "legacy");
        verify(subscriptionRepository, times(1)).findActiveFor(YearMonth.of(2026, 4), SubscriptionState.PRODUCTION, "legacy");
    }

    @Test
    void execute_withInstallments_deductsInstallmentTotalFromRemaining() {
        Wallet wallet = new Wallet(
                "wallet-1",
                "June",
                Money.of("3000.00"),
                Money.of("3000.00"),
                LocalDate.of(2026, 6, 1),
                null,
                false,
                YearMonth.of(2026, 6),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
        when(walletRepository.findAll("owner-1")).thenReturn(List.of(wallet));
        when(subscriptionRepository.findActiveFor(YearMonth.of(2026, 6), SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of());
        when(installmentRepository.findActiveAffectingAny(any(Collection.class), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(installment("1000.00", YearMonth.of(2026, 5))));

        List<WalletOutput> result = useCase.execute("owner-1");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().remaining()).isEqualByComparingTo("2000.00");
    }

    private static Wallet productionWallet(String id, String description, String budget, String remaining) {
        return new Wallet(
                id, description,
                Money.of(budget), Money.of(remaining),
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1), false,
                MAY_2026,
                WalletState.PRODUCTION, FlagEnum.NONE
        );
    }

    private static Subscription subscription(String id, String amount) {
        return Subscription.rebuild(
                id, "sub",
                Currency.getInstance("BRL"),
                MAY_2026, null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(MAY_2026, Money.of(amount))),
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
