package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveIngestWalletUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final Currency BRL = Currency.getInstance("BRL");
    private static final LocalDate TODAY = LocalDate.of(2026, 5, 19);
    private static final YearMonth CURRENT_MONTH = YearMonth.of(2026, 5);

    @Mock
    private WalletRepository walletRepository;

    private ResolveIngestWalletUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveIngestWalletUseCase(walletRepository);
    }

    private Wallet wallet(String id, YearMonth month, boolean closed) {
        return new Wallet(id, OWNER, "W-" + id,
                Money.of(BigDecimal.valueOf(1000), BRL),
                Money.of(BigDecimal.valueOf(1000), BRL),
                month.atDay(1), null, closed, month, WalletState.PRODUCTION, null);
    }

    @Test
    void resolve_noWallets_returnsEmpty() {
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of());

        assertThat(useCase.resolve(OWNER, TODAY)).isEmpty();
    }

    @Test
    void resolve_priority1_currentMonthOpen_returnsIt() {
        Wallet p1 = wallet("p1", CURRENT_MONTH, false);
        Wallet p4 = wallet("p4", CURRENT_MONTH.minusMonths(1), true);
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of(p4, p1));

        Optional<Wallet> result = useCase.resolve(OWNER, TODAY);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("p1");
    }

    @Test
    void resolve_priority2_futureOpen_smallestMonth() {
        Wallet future1 = wallet("future1", CURRENT_MONTH.plusMonths(2), false);
        Wallet future2 = wallet("future2", CURRENT_MONTH.plusMonths(1), false);
        Wallet pastClosed = wallet("past", CURRENT_MONTH.minusMonths(1), true);
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of(future1, future2, pastClosed));

        Optional<Wallet> result = useCase.resolve(OWNER, TODAY);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("future2");
    }

    @Test
    void resolve_priority3_currentMonthClosed_whenNoOpen() {
        Wallet currentClosed = wallet("current-closed", CURRENT_MONTH, true);
        Wallet pastClosed = wallet("past", CURRENT_MONTH.minusMonths(1), true);
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of(pastClosed, currentClosed));

        Optional<Wallet> result = useCase.resolve(OWNER, TODAY);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("current-closed");
    }

    @Test
    void resolve_priority4_pastClosedMostRecent() {
        Wallet past1 = wallet("past1", CURRENT_MONTH.minusMonths(2), true);
        Wallet past2 = wallet("past2", CURRENT_MONTH.minusMonths(1), true);
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of(past1, past2));

        Optional<Wallet> result = useCase.resolve(OWNER, TODAY);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("past2");
    }

    @Test
    void resolve_priority5_fallback_anyMostRecent() {
        // Only future closed wallets — none match P1..P4
        Wallet future1 = wallet("f1", CURRENT_MONTH.plusMonths(3), true);
        Wallet future2 = wallet("f2", CURRENT_MONTH.plusMonths(1), true);
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of(future2, future1));

        Optional<Wallet> result = useCase.resolve(OWNER, TODAY);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("f1");
    }

    @Test
    void resolve_singleWallet_anyState_returnsIt() {
        Wallet only = wallet("only", CURRENT_MONTH.minusMonths(5), true);
        when(walletRepository.findAllProductionByOwnerId(OWNER)).thenReturn(List.of(only));

        assertThat(useCase.resolve(OWNER, TODAY)).contains(only);
    }
}
