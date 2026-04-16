package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindWalletByIdUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    private FindWalletByIdUseCase useCase;

    private static final String WALLET_ID = "wallet-1";

    @BeforeEach
    void setUp() {
        useCase = new FindWalletByIdUseCase(walletRepository);
    }

    @Test
    void findById_happyPath_returnsMappedOutput() {
        Wallet wallet = new Wallet(
                WALLET_ID,
                "Test Wallet",
                Money.of("3000.00"),
                Money.of("2500.00"),
                LocalDate.of(2024, 1, 1),
                null,
                false
        );
        when(walletRepository.findById(WALLET_ID)).thenReturn(Optional.of(wallet));

        WalletOutput result = useCase.findById(WALLET_ID);

        assertThat(result.id()).isEqualTo(WALLET_ID);
        assertThat(result.description()).isEqualTo("Test Wallet");
        assertThat(result.budget()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.remaining()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(result.closedDate()).isNull();
        assertThat(result.isClosed()).isFalse();
    }

    @Test
    void findById_walletNotFound_throwsWalletNotFoundException() {
        when(walletRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById("nonexistent"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void findById_repositoryFails_propagates() {
        RuntimeException boom = new RuntimeException("mongo down");
        when(walletRepository.findById(WALLET_ID)).thenThrow(boom);

        assertThatThrownBy(() -> useCase.findById(WALLET_ID)).isSameAs(boom);
    }
}
