package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindBulletsByWalletIdUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @Mock
    private FindWalletByIdBoundary findWalletByIdBoundary;

    private FindBulletsByWalletIdUseCase useCase;

    private static final String WALLET_ID = "wallet-1";

    @BeforeEach
    void setUp() {
        useCase = new FindBulletsByWalletIdUseCase(bulletRepository, findWalletByIdBoundary);
    }

    @Test
    void execute_happyPath_returnsMappedOutputs() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("2000.00"), new BigDecimal("2000.00"), null, null, false, null, null);
        when(findWalletByIdBoundary.findById(WALLET_ID, "owner-1")).thenReturn(walletOutput);

        Money budget1 = Money.of("1500.00");
        Bullet bullet1 = Bullet.create("Rent", budget1, budget1, WALLET_ID);
        Money budget2 = Money.of("300.00");
        Bullet bullet2 = Bullet.create("Food", budget2, budget2, WALLET_ID);
        when(bulletRepository.findByWalletId(WALLET_ID, "owner-1")).thenReturn(List.of(bullet1, bullet2));

        List<BulletOutput> result = useCase.execute(WALLET_ID, "owner-1");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).description()).isEqualTo("Rent");
        assertThat(result.get(0).budget()).isEqualByComparingTo("1500.00");
        assertThat(result.get(0).walletId()).isEqualTo(WALLET_ID);
        assertThat(result.get(1).description()).isEqualTo("Food");
        assertThat(result.get(1).budget()).isEqualByComparingTo("300.00");
    }

    @Test
    void execute_emptyList_returnsEmptyContent() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("2000.00"), new BigDecimal("2000.00"), null, null, false, null, null);
        when(findWalletByIdBoundary.findById(WALLET_ID, "owner-1")).thenReturn(walletOutput);
        when(bulletRepository.findByWalletId(WALLET_ID, "owner-1")).thenReturn(List.of());

        List<BulletOutput> result = useCase.execute(WALLET_ID, "owner-1");

        assertThat(result).isEmpty();
    }

    @Test
    void execute_walletNotFound_propagatesExceptionWithNoRepositoryCall() {
        when(findWalletByIdBoundary.findById("nonexistent", "owner-1"))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        assertThatThrownBy(() -> useCase.execute("nonexistent", "owner-1"))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(bulletRepository);
    }

    @Test
    void execute_repositoryFails_propagates() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("2000.00"), new BigDecimal("2000.00"), null, null, false, null, null);
        when(findWalletByIdBoundary.findById(WALLET_ID, "owner-1")).thenReturn(walletOutput);

        RuntimeException boom = new RuntimeException("mongo down");
        when(bulletRepository.findByWalletId(WALLET_ID, "owner-1")).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(WALLET_ID, "owner-1")).isSameAs(boom);
    }
}
