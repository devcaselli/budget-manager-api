package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletInUseException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class DeleteBulletByIdUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    private DeleteBulletByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteBulletByIdUseCase(
                bulletRepository,
                walletRepository,
                paymentRepository,
                findWalletDomainByIdBoundary);
    }

    @Test
    void execute_refundsRemainingAndDeletesBullet() {
        Bullet bullet = new Bullet("bullet-1", "rent", Money.of("500.00"), Money.of("320.00"), "wallet-1");
        Wallet wallet = new Wallet("wallet-1", "wallet", Money.of("1000.00"), Money.of("300.00"), null, null, false);

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(bullet));
        when(paymentRepository.existsByBulletId("bullet-1")).thenReturn(false);
        when(findWalletDomainByIdBoundary.findById("wallet-1")).thenReturn(wallet);

        useCase.execute("bullet-1");

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        assertThat(walletCaptor.getValue().getRemaining()).isEqualTo(Money.of("620.00"));

        var inOrder = inOrder(walletRepository, bulletRepository);
        inOrder.verify(walletRepository).save(any(Wallet.class));
        inOrder.verify(bulletRepository).deleteById("bullet-1");
    }

    @Test
    void execute_whenBulletIsMissing_throwsAndDoesNotTouchOtherPorts() {
        when(bulletRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing"))
                .isInstanceOf(BulletNotFoundException.class);

        verifyNoInteractions(walletRepository, paymentRepository, findWalletDomainByIdBoundary);
        verify(bulletRepository, never()).deleteById(any());
    }

    @Test
    void execute_whenBulletHasPayments_throwsAndDoesNotRefundOrDelete() {
        Bullet bullet = new Bullet("bullet-1", "rent", Money.of("500.00"), Money.of("320.00"), "wallet-1");

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(bullet));
        when(paymentRepository.existsByBulletId("bullet-1")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute("bullet-1"))
                .isInstanceOf(BulletInUseException.class);

        verifyNoInteractions(walletRepository, findWalletDomainByIdBoundary);
        verify(bulletRepository, never()).deleteById(any());
    }
}
