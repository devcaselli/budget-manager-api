package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.flag.FlagAwareExecutor;
import br.com.casellisoftware.budgetmanager.application.flag.FlagStrategyRegistry;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test for {@link SaveBulletIgnoreSubscriptionReservationStrategy}.
 * Exercises the full stack: FlagAwareExecutor + FlagStrategyRegistry + real strategies
 * with mocked repositories, ensuring the IGNORE strategy correctly bypasses subscription
 * reservation.
 */
@ExtendWith(MockitoExtension.class)
class SaveBulletIgnoreSubscriptionReservationStrategyTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private ShareRepository shareRepository;

    private SaveBulletUseCase useCase;

    /**
     * Wallet with BULLET_IGNORE_SUBSCRIPTION_RESERVATION flag, budget and remaining
     * equal to 3000.00. When combined with a 2000.00 subscription that would normally
     * be reserved, only 1000.00 would remain available. A 1500.00 bullet would fail
     * without IGNORE, but succeeds with IGNORE because subscriptions are not reserved.
     */
    private Wallet walletWithIgnoreFlag;

    /**
     * Wallet with NONE flag (default), budget and remaining equal to 3000.00. Used to
     * test the default strategy which DOES reserve subscriptions. When subscriptions
     * total 2000.00, only 1000.00 is available for bullets.
     */
    private Wallet walletWithDefaultFlag;

    /**
     * A subscription of 2000.00 that would normally be reserved against the wallet.
     * Created once and reused across tests to ensure consistency.
     */
    private Subscription activeSubscription;

    @BeforeEach
    void setUp() {
        // Set up shared test data: two wallets (one with IGNORE flag, one with NONE) +
        // subscription with matching currency
        walletWithIgnoreFlag = new Wallet(
                "wallet-1",
                "Test Wallet with Ignore Flag",
                Money.of("3000.00"),   // budget in BRL (default)
                Money.of("3000.00"),   // remaining (equals budget initially)
                null,
                null,
                false,
                YearMonth.of(2026, 4),
                WalletState.PRODUCTION,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION  // wallet has IGNORE flag
        );

        walletWithDefaultFlag = new Wallet(
                "wallet-1",
                "Test Wallet with Default Flag",
                Money.of("3000.00"),   // budget in BRL (default)
                Money.of("3000.00"),   // remaining (equals budget initially)
                null,
                null,
                false,
                YearMonth.of(2026, 4),
                WalletState.PRODUCTION,
                FlagEnum.NONE  // wallet has no special flag (default strategy)
        );

        activeSubscription = Subscription.create(
                "Netflix",
                Currency.getInstance("BRL"),  // matching wallet currency (BRL is default)
                Money.of("2000.00"),
                YearMonth.of(2026, 4),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE,
                "cc-test"
        );

        // Lenient stub: subscription repo behavior varies by test
        // Some tests call findActiveFor (default strategy), others don't (ignore strategy)
        lenient().when(subscriptionRepository.findActiveFor(any(), any(), any()))
                .thenReturn(List.of(activeSubscription));
    }

    /**
     * Wires the executor with the IGNORE strategy and a FlagManager that returns
     * true for BULLET_IGNORE_SUBSCRIPTION_RESERVATION.
     */
    private SaveBulletUseCase createUseCaseWithIgnoreStrategyEnabled() {
        DefaultSaveBulletStrategy defaultStrategy = new DefaultSaveBulletStrategy(
                bulletRepository, walletRepository, findWalletDomainByIdBoundary,
                subscriptionRepository, shareRepository);
        SaveBulletIgnoreSubscriptionReservationStrategy ignoreStrategy =
                new SaveBulletIgnoreSubscriptionReservationStrategy(
                        bulletRepository, walletRepository, findWalletDomainByIdBoundary,
                        subscriptionRepository);

        FlagStrategyRegistry<BulletInput, BulletOutput> registry =
                FlagStrategyRegistry.<BulletInput, BulletOutput>builder()
                        .withDefault(defaultStrategy)
                        .register(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, ignoreStrategy)
                        .build();

        // FlagManager that always returns true for BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        FlagManager flagManager = (flag) -> flag == FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION;

        FlagAwareExecutor<BulletInput, BulletOutput> executor =
                new FlagAwareExecutor<>(flagManager, registry);

        return new SaveBulletUseCase(executor, findWalletDomainByIdBoundary);
    }

    /**
     * Wires the executor with the DEFAULT strategy and a FlagManager that always
     * returns false, ensuring subscription reservation IS applied.
     */
    private SaveBulletUseCase createUseCaseWithDefaultStrategyOnly() {
        DefaultSaveBulletStrategy defaultStrategy = new DefaultSaveBulletStrategy(
                bulletRepository, walletRepository, findWalletDomainByIdBoundary,
                subscriptionRepository, shareRepository);

        FlagStrategyRegistry<BulletInput, BulletOutput> registry =
                FlagStrategyRegistry.<BulletInput, BulletOutput>builder()
                        .withDefault(defaultStrategy)
                        .build();

        // FlagManager that always returns false
        FlagManager flagManager = (flag) -> false;

        FlagAwareExecutor<BulletInput, BulletOutput> executor =
                new FlagAwareExecutor<>(flagManager, registry);

        return new SaveBulletUseCase(executor, findWalletDomainByIdBoundary);
    }

    @Test
    void ignoreStrategy_withActiveSubscriptions_skipsReservationAndAllowsBullet() {
        // Given: wallet with IGNORE flag, 3000 remaining, active subscription of 2000
        // Without IGNORE: only 1000 available (3000 - 2000 reserved = 1000 free)
        // With IGNORE: full 3000 available (subscriptions not reserved)
        // Bullet of 1500 succeeds under IGNORE but would fail under DEFAULT
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy"))
                .thenReturn(walletWithIgnoreFlag);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase = createUseCaseWithIgnoreStrategyEnabled();

        BulletInput input = new BulletInput(
                "rent",
                new BigDecimal("1500.00"),
                "wallet-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        // When: execute with IGNORE strategy
        BulletOutput result = useCase.execute(input);

        // Then: bullet is created successfully
        ArgumentCaptor<Bullet> bulletCaptor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(bulletCaptor.capture());

        Bullet savedBullet = bulletCaptor.getValue();
        assertThat(savedBullet.getId()).isNotBlank();
        assertThat(savedBullet.getDescription()).isEqualTo("rent");
        assertThat(savedBullet.getBudget().amount()).isEqualByComparingTo("1500.00");
        assertThat(result.budget()).isEqualByComparingTo("1500.00");

        // Verify subscription repo was NOT called (IGNORE strategy bypasses subscription check)
        verify(subscriptionRepository, never()).findActiveFor(any(), any(), any());

        // Verify wallet was debited only by bullet amount (1500), not by subscriptions
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet debited = walletCaptor.getValue();
        assertThat(debited.getRemaining().amount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void ignoreStrategy_withoutActiveSubscriptions_succeeds() {
        // Given: wallet with IGNORE flag, 3000 remaining. The lenient stub returns an
        // active subscription, but IGNORE strategy never calls findActiveFor, so any
        // subscription data is irrelevant to this test.
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy"))
                .thenReturn(walletWithIgnoreFlag);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase = createUseCaseWithIgnoreStrategyEnabled();

        BulletInput input = new BulletInput(
                "utilities",
                new BigDecimal("500.00"),
                "wallet-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        // When: execute with IGNORE strategy (bypasses subscription check entirely)
        BulletOutput result = useCase.execute(input);

        // Then: bullet is created successfully
        assertThat(result.budget()).isEqualByComparingTo("500.00");
        assertThat(result.description()).isEqualTo("utilities");

        // Verify subscription repo was NOT called (IGNORE strategy bypasses subscription check)
        verify(subscriptionRepository, never()).findActiveFor(any(), any(), any());

        // Verify wallet remaining is correct (3000 - 500)
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet debited = walletCaptor.getValue();
        assertThat(debited.getRemaining().amount()).isEqualByComparingTo("2500.00");
    }

    @Test
    void defaultStrategy_withActiveSubscriptions_appliesReservationAndRejectsInsufficientBullet() {
        // Given: wallet with DEFAULT flag, 3000 remaining, subscription of 2000
        // Default strategy reserves 2000, leaving 1000 available
        // Bullet of 1500 exceeds available (1000), so should fail
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy"))
                .thenReturn(walletWithDefaultFlag);
        when(subscriptionRepository.findActiveFor(
                eq(YearMonth.of(2026, 4)),
                eq(SubscriptionState.PRODUCTION), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(activeSubscription));

        useCase = createUseCaseWithDefaultStrategyOnly();

        BulletInput input = new BulletInput(
                "rent",
                new BigDecimal("1500.00"),
                "wallet-1"
        );

        // When: execute with default strategy (which applies reservation)
        // Then: throws WalletAllocationExceededException
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class);

        // Verify subscriptions were consulted
        verify(subscriptionRepository, times(1)).findActiveFor(
                eq(YearMonth.of(2026, 4)),
                eq(SubscriptionState.PRODUCTION), org.mockito.ArgumentMatchers.anyString());

        // Verify nothing was persisted
        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }

    @Test
    void defaultStrategy_withActiveSubscriptions_allowsSmallerBulletWithinReservedAmount() {
        // Given: wallet with DEFAULT flag, 3000 remaining, subscription of 2000
        // Default strategy reserves 2000, leaving 1000 available
        // Bullet of 800 fits within available (1000), so should succeed
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy"))
                .thenReturn(walletWithDefaultFlag);
        when(subscriptionRepository.findActiveFor(
                eq(YearMonth.of(2026, 4)),
                eq(SubscriptionState.PRODUCTION), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(activeSubscription));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase = createUseCaseWithDefaultStrategyOnly();

        BulletInput input = new BulletInput(
                "food",
                new BigDecimal("800.00"),
                "wallet-1"
        );

        // When: execute with default strategy
        BulletOutput result = useCase.execute(input);

        // Then: bullet is created successfully
        assertThat(result.budget()).isEqualByComparingTo("800.00");
        assertThat(result.description()).isEqualTo("food");

        // Verify wallet remaining reflects only bullet debit (3000 - 800)
        // Subscription reservation does not reduce wallet.remaining; it's checked during validation
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet debited = walletCaptor.getValue();
        assertThat(debited.getRemaining().amount()).isEqualByComparingTo("2200.00");
    }

    @Test
    void ignoreStrategyDispatches_whenWalletFlagIsIGNORE_regardlessOfInputFlag() {
        // Case (a): Wallet flag = IGNORE, input flag = NONE
        // Proves dispatch is wallet-driven: even with input flag = NONE, the IGNORE
        // strategy must dispatch because the wallet carries the IGNORE flag.
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy"))
                .thenReturn(walletWithIgnoreFlag);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase = createUseCaseWithIgnoreStrategyEnabled();

        // Input has NONE flag (not IGNORE), but wallet has IGNORE flag
        BulletInput inputWithNoneFlag = new BulletInput(
                "rent",
                new BigDecimal("1500.00"),
                "wallet-1"
                // implicitly flag = FlagEnum.NONE
        );

        // When: execute with wallet carrying IGNORE flag and input = NONE
        BulletOutput result = useCase.execute(inputWithNoneFlag);

        // Then: succeeds because IGNORE strategy is dispatched (wallet-driven)
        assertThat(result.budget()).isEqualByComparingTo("1500.00");

        // Verify subscriptions were NOT called (IGNORE strategy bypasses subscription check)
        verify(subscriptionRepository, never()).findActiveFor(any(), any(), any());

        // Verify wallet debited only by bullet amount (1500), not by subscriptions
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet debited = walletCaptor.getValue();
        assertThat(debited.getRemaining().amount()).isEqualByComparingTo("1500.00");
    }

    @Test
    void defaultStrategyDispatches_whenWalletFlagIsNONE_regardlessOfInputFlag() {
        // Case (b): Wallet flag = NONE, input flag = IGNORE
        // Proves input flag is NOT used for dispatch: even with input flag = IGNORE,
        // the DEFAULT strategy dispatches because the wallet carries the NONE flag.
        // This confirms the FlagAwareExecutor reads the wallet's flag, not the input.
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy"))
                .thenReturn(walletWithDefaultFlag);
        when(subscriptionRepository.findActiveFor(
                eq(YearMonth.of(2026, 4)),
                eq(SubscriptionState.PRODUCTION), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(List.of(activeSubscription));

        useCase = createUseCaseWithIgnoreStrategyEnabled();

        // Input has IGNORE flag, but wallet has NONE flag
        BulletInput inputWithIgnoreFlag = new BulletInput(
                "rent",
                new BigDecimal("1500.00"),
                "wallet-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        // When: execute with wallet carrying NONE flag and input = IGNORE
        // Then: throws WalletAllocationExceededException because DEFAULT strategy
        // (wallet-driven, not input-driven) applies subscription reservation
        assertThatThrownBy(() -> useCase.execute(inputWithIgnoreFlag))
                .isInstanceOf(WalletAllocationExceededException.class);

        // Verify subscriptions were consulted (DEFAULT strategy applies reservation)
        verify(subscriptionRepository, times(1)).findActiveFor(
                eq(YearMonth.of(2026, 4)),
                eq(SubscriptionState.PRODUCTION), org.mockito.ArgumentMatchers.anyString());

        // Verify nothing was persisted
        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }
}
