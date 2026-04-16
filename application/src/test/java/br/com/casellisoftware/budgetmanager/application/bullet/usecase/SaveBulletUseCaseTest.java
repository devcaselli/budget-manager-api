package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveBulletUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private FindWalletByIdBoundary findWalletByIdBoundary;

    private SaveBulletUseCase useCase;

    private BulletInput input;

    @BeforeEach
    void setUp() {
        useCase = new SaveBulletUseCase(bulletRepository, findWalletByIdBoundary);
        input = new BulletInput("rent", new BigDecimal("1500.00"), "wallet-1");
    }

    private void stubWalletExists() {
        WalletOutput walletOutput = new WalletOutput("wallet-1", "Test Wallet",
                new BigDecimal("5000.00"), new BigDecimal("5000.00"), null, null, false);
        when(findWalletByIdBoundary.findById("wallet-1")).thenReturn(walletOutput);
    }

    @Test
    void execute_happyPath_createsDomainPersistsAndReturnsOutput() {
        stubWalletExists();
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        BulletOutput result = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());

        Bullet persisted = captor.getValue();
        assertThat(persisted.getId()).isNotBlank();
        assertThat(persisted.getWalletId()).isEqualTo("wallet-1");
        assertThat(persisted.getDescription()).isEqualTo("rent");
        assertThat(persisted.getBudget().amount()).isEqualByComparingTo("1500.00");
        assertThat(persisted.getRemaining().amount()).isEqualByComparingTo("1500.00");

        assertThat(result.id()).isEqualTo(persisted.getId());
        assertThat(result.description()).isEqualTo("rent");
        assertThat(result.budget()).isEqualByComparingTo("1500.00");
        assertThat(result.remaining()).isEqualByComparingTo("1500.00");
        assertThat(result.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_walletNotFound_propagatesAndDoesNotTouchRepository() {
        when(findWalletByIdBoundary.findById("nonexistent"))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        BulletInput invalidInput = new BulletInput("rent", new BigDecimal("1500.00"), "nonexistent");

        assertThatThrownBy(() -> useCase.execute(invalidInput))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(bulletRepository);
    }

    @Test
    void execute_whenRepositoryFails_propagates() {
        stubWalletExists();
        RuntimeException boom = new RuntimeException("mongo down");
        when(bulletRepository.save(any(Bullet.class))).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);
    }
}
