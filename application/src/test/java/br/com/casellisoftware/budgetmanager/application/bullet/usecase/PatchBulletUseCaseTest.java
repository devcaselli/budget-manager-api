package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.flag.FlagAwareExecutor;
import br.com.casellisoftware.budgetmanager.application.flag.FlagStrategyRegistry;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
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
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link PatchBulletUseCase} (flag dispatch) and its underlying
 * {@link DefaultPatchBulletStrategy} (patch logic).
 *
 * <p>Logic tests exercise the strategy directly; dispatch tests verify that
 * {@code PatchBulletUseCase} resolves the wallet flag and routes to the executor.</p>
 */
@ExtendWith(MockitoExtension.class)
class PatchBulletUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private FlagManager flagManager;

    @Mock
    private ShareRepository shareRepository;

    /**
     * Default patch strategy wired with a no-flag FlagManager so all tests use
     * the default (subscription-aware) strategy behaviour, with a mocked subscription
     * repository that returns no subscriptions.
     */
    private DefaultPatchBulletStrategy defaultStrategy;
    private PatchBulletUseCase useCase;

    @BeforeEach
    void setUp() {
        lenient().when(subscriptionRepository.findActiveFor(any(), any(), any())).thenReturn(List.of());
        defaultStrategy = new DefaultPatchBulletStrategy(
                bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository, shareRepository);

        FlagStrategyRegistry<PatchBulletInput, BulletOutput> registry =
                new FlagStrategyRegistry<>(defaultStrategy, null);
        FlagAwareExecutor<PatchBulletInput, BulletOutput> executor =
                new FlagAwareExecutor<>(flagManager, registry);
        useCase = new PatchBulletUseCase(bulletRepository, executor, findWalletDomainByIdBoundary);
    }

    // -----------------------------------------------------------------------
    // Logic tests — exercised through the strategy (same bullet is found twice:
    // once in PatchBulletUseCase to resolve wallet flag, once in the strategy)
    // -----------------------------------------------------------------------

    private Bullet existingBullet() {
        return new Bullet("bullet-1", "rent", Money.of("500.00"), Money.of("320.00"), "wallet-1");
    }

    private Wallet productionWallet(String remaining) {
        return new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of(remaining),
                null, null, false, YearMonth.of(2026, 4), WalletState.PRODUCTION, FlagEnum.NONE);
    }

    @Test
    void execute_whenBudgetIncreases_debitsWalletAndPreservesConsumedAmount() {
        Bullet existing = existingBullet();
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", "groceries", new BigDecimal("650.00"), null, "wallet-1");
        Wallet wallet = productionWallet("300.00");

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flagManager.isEnabled(any())).thenReturn(false);

        BulletOutput output = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Bullet saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo("bullet-1");
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        assertThat(saved.getDescription()).isEqualTo("groceries");
        assertThat(saved.getBudget()).isEqualTo(Money.of("650.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("470.00"));
        assertThat(saved.consumed()).isEqualTo(Money.of("180.00"));
        assertThat(walletCaptor.getValue().getRemaining()).isEqualTo(Money.of("150.00"));

        var inOrder = inOrder(walletRepository, bulletRepository);
        inOrder.verify(walletRepository).save(any(Wallet.class));
        inOrder.verify(bulletRepository).save(any(Bullet.class));

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.description()).isEqualTo(saved.getDescription());
        assertThat(output.budget()).isEqualByComparingTo("650.00");
        assertThat(output.remaining()).isEqualByComparingTo("470.00");
        assertThat(output.walletId()).isEqualTo(saved.getWalletId());
    }

    @Test
    void execute_whenBudgetDecreases_creditsWalletAndPreservesConsumedAmount() {
        Bullet existing = existingBullet();
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("400.00"), null, null);
        Wallet wallet = productionWallet("300.00");

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flagManager.isEnabled(any())).thenReturn(false);

        useCase.execute(input);

        ArgumentCaptor<Bullet> bulletCaptor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(bulletCaptor.capture());
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());

        assertThat(bulletCaptor.getValue().getBudget()).isEqualTo(Money.of("400.00"));
        assertThat(bulletCaptor.getValue().getRemaining()).isEqualTo(Money.of("220.00"));
        assertThat(bulletCaptor.getValue().consumed()).isEqualTo(Money.of("180.00"));
        assertThat(walletCaptor.getValue().getRemaining()).isEqualTo(Money.of("400.00"));
    }

    @Test
    void execute_whenOnlyDescriptionProvided_preservesFinancialFields() {
        Bullet existing = existingBullet();
        Wallet wallet = productionWallet("300.00");
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", "groceries", null, null, null);

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flagManager.isEnabled(any())).thenReturn(false);

        useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        Bullet saved = captor.getValue();

        assertThat(saved.getDescription()).isEqualTo("groceries");
        assertThat(saved.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("320.00"));
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        verifyNoInteractions(walletRepository);
    }

    @Test
    void execute_whenBulletDoesNotExist_throwsAndDoesNotSave() {
        PatchBulletInput input = new PatchBulletInput(
                "bullet-missing", "groceries", null, null, null);
        when(bulletRepository.findById("bullet-missing", "legacy")).thenReturn(Optional.empty());
        // flagManager is not reached because the use case throws before dispatching

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(BulletNotFoundException.class)
                .hasMessageContaining("bullet-missing");

        verify(bulletRepository, never()).save(any());
        verifyNoInteractions(walletRepository);
    }

    @Test
    void execute_whenPatchIsEmpty_savesCurrentStateUnchanged() {
        Bullet existing = existingBullet();
        Wallet wallet = productionWallet("300.00");
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, null, null, null);

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flagManager.isEnabled(any())).thenReturn(false);

        BulletOutput output = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        verifyNoInteractions(walletRepository);
        assertThat(output.id()).isEqualTo("bullet-1");
        assertThat(output.description()).isEqualTo("rent");
        assertThat(output.budget()).isEqualByComparingTo("500.00");
        assertThat(output.remaining()).isEqualByComparingTo("320.00");
        assertThat(output.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_whenWalletIdChanges_throwsAndDoesNotSave() {
        Bullet existing = existingBullet();
        Wallet wallet = productionWallet("300.00");
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, null, null, "wallet-2");

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(flagManager.isEnabled(any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId is immutable");

        verify(bulletRepository, never()).save(any());
        verifyNoInteractions(walletRepository);
    }

    @Test
    void execute_whenBudgetIncreaseExceedsWalletRemaining_doesNotSave() {
        Bullet existing = existingBullet();
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("650.00"), null, null);
        Wallet wallet = productionWallet("100.00");

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(flagManager.isEnabled(any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class);

        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }

    @Test
    void execute_whenBudgetReductionCutsConsumedAmount_throwsSemanticException() {
        Bullet existing = existingBullet();
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("100.00"), null, null);
        Wallet wallet = productionWallet("300.00");

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(flagManager.isEnabled(any())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class)
                .hasMessageContaining("already consumed");

        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // Dispatch test — verifies flag is resolved from the persisted wallet
    // -----------------------------------------------------------------------

    @Test
    void execute_dispatchesToExecutorUsingWalletFlag() {
        Bullet existing = new Bullet("bullet-1", "rent",
                Money.of("500.00"), Money.of("500.00"), "wallet-1");
        Wallet wallet = new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of("500.00"),
                null, null, false, YearMonth.of(2026, 4), WalletState.PRODUCTION,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", "groceries", null, null, null);

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        // Flag is enabled so the ignore-reservation strategy would be selected,
        // but we only verify the dispatch direction here.
        when(flagManager.isEnabled(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION)).thenReturn(true);

        BulletOutput output = useCase.execute(input);

        assertThat(output.id()).isEqualTo("bullet-1");
        // walletRepository should NOT be called (no budget change, description only)
        verifyNoInteractions(walletRepository);
    }

    @Test
    void execute_withNonZeroSubscriptionTotal_reconcileWalletConsidersReservedAmountInValidation() {
        // ACCEPT-PATH TEST: Tight fixture where subscriptions MUST be considered.
        // Wallet remaining = 250, bullet current = 400, requested = 500 (delta = 100)
        // Reserved subs = 150
        // Effective check: delta + reserved = 100 + 150 = 250 (NOT > 250) => PASSES
        // If delta were 101: 101 + 150 = 251 > 250 => would FAIL
        // This proves the strategy must consult subscriptions to allow this accept.

        Bullet existing = new Bullet("bullet-1", "rent", Money.of("400.00"),
                Money.of("400.00"), "wallet-1");
        Wallet wallet = productionWallet("250.00");  // wallet remaining MUST be 250

        // Create a subscription that reserves 150.00 (the critical amount)
        Subscription subscription = Subscription.create(
                "Critical Subscription",
                wallet.getBudget().currency(),
                Money.of("150.00"),
                wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE,
                "cc-test"
        );

        // Input: increase bullet budget from 400 to 500 (delta of +100)
        // This is exactly at the boundary: 100 (delta) + 150 (reserved) = 250 (wallet remaining)
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("500.00"), null, null);

        // Mocks
        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(subscriptionRepository.findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(subscription));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flagManager.isEnabled(any())).thenReturn(false);

        // Execute: patch the bullet
        BulletOutput output = useCase.execute(input);

        // Verify: wallet was debited by delta (100), remaining becomes 150
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertThat(savedWallet.getRemaining()).isEqualTo(Money.of("150.00"));

        // Verify bullet was saved with new budget (500) and adjusted remaining
        // When budget increases by 100, remaining also increases by 100 (from 400 to 500)
        ArgumentCaptor<Bullet> bulletCaptor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(bulletCaptor.capture());
        Bullet savedBullet = bulletCaptor.getValue();
        assertThat(savedBullet.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(savedBullet.getRemaining()).isEqualTo(Money.of("500.00"));

        // CRITICAL: Verify that subscriptionRepository.findActiveFor was invoked.
        // This proves the strategy actually consulted subscriptions for validation.
        verify(subscriptionRepository).findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy");
    }

    @Test
    void execute_withNonZeroSubscriptionTotal_acceptWhenSubscriptionsZero() {
        // ACCEPT-PATH WITHOUT SUBSCRIPTIONS: Same fixture numbers but with no subscriptions.
        // Wallet remaining = 250, delta = 100, reserved = 0
        // Effective check: delta + reserved = 100 + 0 = 100 (NOT > 250) => PASSES
        // This proves the accept path works whether subscriptions are present or absent.

        Bullet existing = new Bullet("bullet-1", "rent", Money.of("400.00"),
                Money.of("400.00"), "wallet-1");
        Wallet wallet = productionWallet("250.00");

        // No subscriptions returned from repo
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("500.00"), null, null);

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(subscriptionRepository.findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of());  // empty: no subscriptions
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(flagManager.isEnabled(any())).thenReturn(false);

        // Execute
        BulletOutput output = useCase.execute(input);

        // Verify: same accept behavior as the subscriptions-present case
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet savedWallet = walletCaptor.getValue();
        assertThat(savedWallet.getRemaining()).isEqualTo(Money.of("150.00"));

        verify(subscriptionRepository).findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy");
    }

    @Test
    void execute_withHighSubscriptionTotal_rejectWhenDeltaPlusSusExceedsRemaining() {
        // REJECT-PATH WITH HIGH SUBSCRIPTIONS: Proves subscriptions prevent accept.
        // Wallet remaining = 250, delta = 100, reserved = 151
        // Effective check: delta + reserved = 100 + 151 = 251 > 250 => FAILS
        // This contrast proves subscriptions are the deciding factor in rejecting this allocation.

        Bullet existing = new Bullet("bullet-1", "rent", Money.of("400.00"),
                Money.of("400.00"), "wallet-1");
        Wallet wallet = productionWallet("250.00");

        // Subscription just over the boundary
        Subscription subscription = Subscription.create(
                "High Subscription",
                wallet.getBudget().currency(),
                Money.of("151.00"),  // One dollar more than the boundary
                wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE,
                "cc-test"
        );

        // Same delta (100) that was accepted before
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("500.00"), null, null);

        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(subscriptionRepository.findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(subscription));
        when(flagManager.isEnabled(any())).thenReturn(false);

        // Execute and verify exception
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class);

        // Verify repos were not touched for save
        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());

        // Verify subscriptionRepository was still consulted
        verify(subscriptionRepository).findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy");
    }

    @Test
    void execute_withNonZeroSubscriptionTotal_rejectsAllocationIfRemainingMinusSubsGoesNegative() {
        // Setup: wallet with budget 1000, remaining 500; bullet with budget 300
        Bullet existing = existingBullet();
        Wallet wallet = productionWallet("500.00");

        // Create a subscription that reserves 300.00 from the wallet
        Subscription subscription = Subscription.create(
                "Premium Service",
                wallet.getBudget().currency(),
                Money.of("300.00"),
                wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE,
                "cc-test"
        );

        // Input: increase bullet budget from 500 to 850 (delta of +350)
        // This would leave wallet remaining at 150, but with 300 reserved for subscriptions,
        // effective remaining would be -150 (violates policy)
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1", null, new BigDecimal("850.00"), null, null);

        // Mocks
        when(bulletRepository.findById("bullet-1", "legacy")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(subscriptionRepository.findActiveFor(wallet.getEffectiveMonth(),
                SubscriptionState.PRODUCTION, "legacy"))
                .thenReturn(List.of(subscription));
        when(flagManager.isEnabled(any())).thenReturn(false);

        // Execute and verify exception
        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class);

        // Verify neither repo was touched for save
        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }
}
