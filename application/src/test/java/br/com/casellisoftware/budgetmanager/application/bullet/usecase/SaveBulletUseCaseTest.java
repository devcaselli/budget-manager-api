package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveBulletUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    private SaveBulletUseCase useCase;

    private BulletInput input;

    @BeforeEach
    void setUp() {
        useCase = new SaveBulletUseCase(bulletRepository, walletRepository, findWalletDomainByIdBoundary);
        input = new BulletInput("rent", new BigDecimal("1500.00"), "wallet-1");
    }

    private void stubWalletExists() {
        Wallet wallet = new Wallet("wallet-1", "Test Wallet",
                Money.of("5000.00"), Money.of("5000.00"), null, null, false);
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);
    }

    @Test
    void execute_happyPath_createsDomainPersistsAndReturnsOutput() {
        stubWalletExists();
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        BulletOutput result = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());

        Bullet persisted = captor.getValue();
        Wallet debited = walletCaptor.getValue();
        assertThat(persisted.getId()).isNotBlank();
        assertThat(persisted.getWalletId()).isEqualTo("wallet-1");
        assertThat(persisted.getDescription()).isEqualTo("rent");
        assertThat(persisted.getBudget().amount()).isEqualByComparingTo("1500.00");
        assertThat(persisted.getRemaining().amount()).isEqualByComparingTo("1500.00");
        assertThat(debited.getRemaining()).isEqualTo(Money.of("3500.00"));

        var inOrder = inOrder(walletRepository, bulletRepository);
        inOrder.verify(walletRepository).save(any(Wallet.class));
        inOrder.verify(bulletRepository).save(any(Bullet.class));

        assertThat(result.id()).isEqualTo(persisted.getId());
        assertThat(result.description()).isEqualTo("rent");
        assertThat(result.budget()).isEqualByComparingTo("1500.00");
        assertThat(result.remaining()).isEqualByComparingTo("1500.00");
        assertThat(result.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_walletNotFound_propagatesAndDoesNotTouchRepository() {
        when(findWalletDomainByIdBoundary.findById("nonexistent"))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        BulletInput invalidInput = new BulletInput("rent", new BigDecimal("1500.00"), "nonexistent");

        assertThatThrownBy(() -> useCase.execute(invalidInput))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(walletRepository, bulletRepository);
    }

    @Test
    void execute_whenBudgetExceedsWalletRemaining_doesNotPersist() {
        Wallet wallet = new Wallet("wallet-1", "Test Wallet",
                Money.of("5000.00"), Money.of("1000.00"), null, null, false);
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletAllocationExceededException.class);

        verify(walletRepository, never()).save(any());
        verify(bulletRepository, never()).save(any());
    }

    @Test
    void execute_whenRepositoryFails_propagates() {
        stubWalletExists();
        RuntimeException boom = new RuntimeException("mongo down");
        when(walletRepository.save(any(Wallet.class))).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);
        verify(bulletRepository, never()).save(any());
    }
}
