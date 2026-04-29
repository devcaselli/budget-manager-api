package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllWalletsUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    private FindAllWalletsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindAllWalletsUseCase(walletRepository);
    }

    @Test
    void execute_returnsMappedWallets() {
        Wallet wallet = new Wallet(
                "wallet-1",
                "Main",
                Money.of("3000.00"),
                Money.of("1250.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        when(walletRepository.findAll()).thenReturn(List.of(wallet));

        List<WalletOutput> result = useCase.execute();

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
        when(walletRepository.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }
}
