package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.NoSubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindWalletByIdUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private ShareRepository shareRepository;

    private FindWalletByIdUseCase useCase;

    private static final String WALLET_ID = "wallet-1";

    @BeforeEach
    void setUp() {
        lenient().when(installmentRepository.findActiveAffecting(any(YearMonth.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        useCase = new FindWalletByIdUseCase(walletRepository, NoSubscriptionRepository.INSTANCE, installmentRepository, shareRepository);
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
                false,
                YearMonth.of(2024, 1),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
        when(walletRepository.findById(WALLET_ID, "owner-1")).thenReturn(Optional.of(wallet));

        WalletOutput result = useCase.findById(WALLET_ID, "owner-1");

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
        when(walletRepository.findById("nonexistent", "owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById("nonexistent", "owner-1"))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void findById_repositoryFails_propagates() {
        RuntimeException boom = new RuntimeException("mongo down");
        when(walletRepository.findById(WALLET_ID, "owner-1")).thenThrow(boom);

        assertThatThrownBy(() -> useCase.findById(WALLET_ID, "owner-1")).isSameAs(boom);
    }

    @Test
    void findById_futureWallet_deductsActiveInstallmentFromOutputOnly() {
        Wallet wallet = new Wallet(
                WALLET_ID,
                "June Wallet",
                Money.of("3000.00"),
                Money.of("3000.00"),
                LocalDate.of(2026, 6, 1),
                null,
                false,
                YearMonth.of(2026, 6),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
        when(walletRepository.findById(WALLET_ID, "owner-1")).thenReturn(Optional.of(wallet));
        when(installmentRepository.findActiveAffecting(YearMonth.of(2026, 6), "legacy"))
                .thenReturn(List.of(installment("1000.00", YearMonth.of(2026, 5))));

        WalletOutput result = useCase.findById(WALLET_ID, "owner-1");

        assertThat(result.remaining()).isEqualByComparingTo("2000.00");
    }

    private static Installment installment(String amount, YearMonth sourceMonth) {
        return Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00"), Currency.getInstance("BRL")),
                Money.of(new BigDecimal(amount), Currency.getInstance("BRL")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "wallet-source",
                sourceMonth,
                FlagEnum.NONE
        );
    }
}
