package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.BulletNotInWalletException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveExtraBudgetUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String WALLET_ID = "wallet-1";
    private static final String BULLET_A = "bullet-1";
    private static final String BULLET_B = "bullet-2";

    @Mock
    private ExtraBudgetRepository extraBudgetRepository;

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private WalletRepository walletRepository;

    private SaveExtraBudgetUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveExtraBudgetUseCase(extraBudgetRepository, bulletRepository, walletRepository);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Wallet wallet() {
        return new Wallet(WALLET_ID, "Test Wallet",
                Money.of("5000.00"), Money.of("5000.00"),
                null, null, false, YearMonth.of(2026, 5), WalletState.PRODUCTION, FlagEnum.NONE);
    }

    private Bullet bullet(String id, String walletId, Money budget, Money remaining) {
        return new Bullet(id, OWNER_ID, "desc-" + id, budget, remaining, walletId, FlagEnum.NONE);
    }

    private void stubWallet() {
        when(walletRepository.findById(WALLET_ID, OWNER_ID)).thenReturn(Optional.of(wallet()));
    }

    private void stubBullet(String bulletId, Money budget, Money remaining) {
        when(bulletRepository.findById(bulletId, OWNER_ID))
                .thenReturn(Optional.of(bullet(bulletId, WALLET_ID, budget, remaining)));
    }

    private ExtraBudgetInput singleAllocationInput() {
        return new ExtraBudgetInput(
                "bonus", WALLET_ID,
                new BigDecimal("200.00"),
                List.of(new AllocationInput(BULLET_A, new BigDecimal("200.00"))),
                OWNER_ID
        );
    }

    private ExtraBudgetInput multiAllocationInput() {
        return new ExtraBudgetInput(
                "bonus", WALLET_ID,
                new BigDecimal("300.00"),
                List.of(
                        new AllocationInput(BULLET_A, new BigDecimal("200.00")),
                        new AllocationInput(BULLET_B, new BigDecimal("100.00"))
                ),
                OWNER_ID
        );
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void save_validSingleAllocation_creditsBulletAndPersists() {
        stubWallet();
        stubBullet(BULLET_A, Money.of("500.00"), Money.of("300.00"));
        when(bulletRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(extraBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExtraBudgetOutput output = useCase.execute(singleAllocationInput());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Bullet>> bulletCaptor = ArgumentCaptor.forClass(List.class);
        verify(bulletRepository).saveAll(bulletCaptor.capture());
        Bullet saved = bulletCaptor.getValue().get(0);
        assertThat(saved.getBudget()).isEqualTo(Money.of("700.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("500.00"));
        assertThat(output).isNotNull();
        assertThat(output.allocations()).hasSize(1);
    }

    @Test
    void save_validMultipleAllocations_creditsAllBulletsAndPersists() {
        stubWallet();
        stubBullet(BULLET_A, Money.of("500.00"), Money.of("300.00"));
        stubBullet(BULLET_B, Money.of("400.00"), Money.of("400.00"));
        when(bulletRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(extraBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ExtraBudgetOutput output = useCase.execute(multiAllocationInput());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Bullet>> captor = ArgumentCaptor.forClass(List.class);
        verify(bulletRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(output.allocations()).hasSize(2);
    }

    @Test
    void save_walletNotFound_throwsWalletNotFoundException() {
        when(walletRepository.findById(WALLET_ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(singleAllocationInput()))
                .isInstanceOf(WalletNotFoundException.class);

        verify(bulletRepository, never()).findById(any(), any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_bulletNotFound_throwsBulletNotFoundException() {
        stubWallet();
        when(bulletRepository.findById(BULLET_A, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(singleAllocationInput()))
                .isInstanceOf(BulletNotFoundException.class);

        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_bulletFromAnotherWallet_throwsBulletNotInWalletException() {
        stubWallet();
        Bullet wrongWalletBullet = bullet(BULLET_A, "wallet-other", Money.of("500.00"), Money.of("300.00"));
        when(bulletRepository.findById(BULLET_A, OWNER_ID)).thenReturn(Optional.of(wrongWalletBullet));

        assertThatThrownBy(() -> useCase.execute(singleAllocationInput()))
                .isInstanceOf(BulletNotInWalletException.class)
                .hasMessageContaining(WALLET_ID);

        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_bulletFromAnotherOwner_throwsBulletNotFoundException() {
        stubWallet();
        // findById with ownerId filter returns empty when bullet belongs to another owner
        when(bulletRepository.findById(BULLET_A, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(singleAllocationInput()))
                .isInstanceOf(BulletNotFoundException.class);

        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_sumDoesNotMatchAmount_throwsIllegalArgument() {
        stubWallet();
        stubBullet(BULLET_A, Money.of("500.00"), Money.of("300.00"));
        stubBullet(BULLET_B, Money.of("400.00"), Money.of("400.00"));

        // allocations sum = 250+100=350, amount = 300 → mismatch
        ExtraBudgetInput mismatch = new ExtraBudgetInput(
                "bonus", WALLET_ID,
                new BigDecimal("300.00"),
                List.of(
                        new AllocationInput(BULLET_A, new BigDecimal("250.00")),
                        new AllocationInput(BULLET_B, new BigDecimal("100.00"))
                ),
                OWNER_ID
        );

        assertThatThrownBy(() -> useCase.execute(mismatch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum of allocations");

        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_duplicateBulletIdInAllocations_throwsIllegalArgument() {
        stubWallet();
        stubBullet(BULLET_A, Money.of("500.00"), Money.of("300.00"));

        ExtraBudgetInput duplicate = new ExtraBudgetInput(
                "bonus", WALLET_ID,
                new BigDecimal("400.00"),
                List.of(
                        new AllocationInput(BULLET_A, new BigDecimal("200.00")),
                        new AllocationInput(BULLET_A, new BigDecimal("200.00"))
                ),
                OWNER_ID
        );

        assertThatThrownBy(() -> useCase.execute(duplicate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate bulletId");

        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_emptyAllocations_throwsIllegalArgument() {
        stubWallet();

        ExtraBudgetInput empty = new ExtraBudgetInput(
                "bonus", WALLET_ID,
                new BigDecimal("300.00"),
                List.of(),
                OWNER_ID
        );

        assertThatThrownBy(() -> useCase.execute(empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocations must not be empty");

        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void save_persistsExtraBudgetAfterAllBulletCredits() {
        stubWallet();
        stubBullet(BULLET_A, Money.of("500.00"), Money.of("300.00"));
        stubBullet(BULLET_B, Money.of("400.00"), Money.of("400.00"));
        when(bulletRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(extraBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(multiAllocationInput());

        InOrder order = inOrder(bulletRepository, extraBudgetRepository);
        order.verify(bulletRepository).saveAll(any());
        order.verify(extraBudgetRepository).save(any(ExtraBudget.class));
    }

    @Test
    void save_emitsCorrectBulletPatchValues() {
        stubWallet();
        stubBullet(BULLET_A, Money.of("500.00"), Money.of("300.00")); // +200 → budget=700, remaining=500
        stubBullet(BULLET_B, Money.of("400.00"), Money.of("200.00")); // +100 → budget=500, remaining=300
        when(bulletRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(extraBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(multiAllocationInput());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Bullet>> captor = ArgumentCaptor.forClass(List.class);
        verify(bulletRepository).saveAll(captor.capture());

        List<Bullet> saved = captor.getValue();
        Bullet savedA = saved.stream().filter(b -> b.getId().equals(BULLET_A)).findFirst().orElseThrow();
        Bullet savedB = saved.stream().filter(b -> b.getId().equals(BULLET_B)).findFirst().orElseThrow();

        assertThat(savedA.getBudget()).isEqualTo(Money.of("700.00"));
        assertThat(savedA.getRemaining()).isEqualTo(Money.of("500.00"));
        assertThat(savedB.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(savedB.getRemaining()).isEqualTo(Money.of("300.00"));
    }
}
