package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchBulletUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    private PatchBulletUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchBulletUseCase(bulletRepository, walletRepository, findWalletDomainByIdBoundary);
    }

    @Test
    void execute_whenBudgetIncreases_debitsWalletAndPreservesConsumedAmount() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                "groceries",
                new BigDecimal("650.00"),
                null,
                "wallet-1"
        );
        Wallet wallet = new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of("300.00"), null, null, false);

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

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
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                new BigDecimal("400.00"),
                null,
                null
        );
        Wallet wallet = new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of("300.00"), null, null, false);

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

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
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                "groceries",
                null,
                null,
                null
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        Bullet saved = captor.getValue();

        assertThat(saved.getDescription()).isEqualTo("groceries");
        assertThat(saved.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("320.00"));
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        verifyNoInteractions(walletRepository, findWalletDomainByIdBoundary);
    }

    @Test
    void execute_whenBulletDoesNotExist_throwsAndDoesNotSave() {
        PatchBulletInput input = new PatchBulletInput(
                "bullet-missing",
                "groceries",
                null,
                null,
                null
        );
        when(bulletRepository.findById("bullet-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(BulletNotFoundException.class)
                .hasMessageContaining("bullet-missing");

        verify(bulletRepository, never()).save(any());
        verifyNoInteractions(walletRepository, findWalletDomainByIdBoundary);
    }

    @Test
    void execute_whenPatchIsEmpty_savesCurrentStateUnchanged() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                null,
                null,
                null
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        BulletOutput output = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        verifyNoInteractions(walletRepository, findWalletDomainByIdBoundary);
        assertThat(output.id()).isEqualTo("bullet-1");
        assertThat(output.description()).isEqualTo("rent");
        assertThat(output.budget()).isEqualByComparingTo("500.00");
        assertThat(output.remaining()).isEqualByComparingTo("320.00");
        assertThat(output.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_whenWalletIdChanges_throwsAndDoesNotSave() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                null,
                null,
                "wallet-2"
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId is immutable");

        verify(bulletRepository, never()).save(any());
        verifyNoInteractions(walletRepository, findWalletDomainByIdBoundary);
    }

    @Test
    void execute_whenBudgetIncreaseExceedsWalletRemaining_doesNotSave() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                new BigDecimal("650.00"),
                null,
                null
        );
        Wallet wallet = new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of("100.00"), null, null, false);

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class);

        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }

    @Test
    void execute_whenBudgetReductionCutsConsumedAmount_throwsSemanticException() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                new BigDecimal("100.00"),
                null,
                null
        );
        Wallet wallet = new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of("300.00"), null, null, false);

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class)
                .hasMessageContaining("already consumed");

        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }
}
