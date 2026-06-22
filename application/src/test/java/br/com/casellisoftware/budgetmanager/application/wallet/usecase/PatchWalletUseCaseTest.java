package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private ShareRepository shareRepository;

    @Mock
    private ReservedBudgetRepository reservedBudgetRepository;

    private PatchWalletUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDate.of(2026, 5, 6).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        lenient().when(subscriptionRepository.findActiveFor(any(YearMonth.class), any(SubscriptionState.class)))
                .thenReturn(List.of());
        lenient().when(installmentRepository.findActiveAffecting(any(YearMonth.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        lenient().when(reservedBudgetRepository.findActiveFor(any(YearMonth.class), org.mockito.ArgumentMatchers.anyString())).thenReturn(List.of());
        useCase = new PatchWalletUseCase(walletRepository, subscriptionRepository, installmentRepository, shareRepository, reservedBudgetRepository, clock);
    }

    @Test
    void execute_patchesOnlyPatchableFields() {
        Wallet existing = new Wallet(
                "wallet-1",
                "monthly",
                Money.of("1000.00"),
                Money.of("700.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                false,
                YearMonth.of(2026, 4),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
        PatchWalletInput input = new PatchWalletInput(
                "wallet-1",
                "may",
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true,
                null
        );

        when(walletRepository.findById("wallet-1", "legacy")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        WalletOutput output = useCase.execute(input);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        Wallet saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo("wallet-1");
        assertThat(saved.getDescription()).isEqualTo("may");
        assertThat(saved.getBudget()).isEqualTo(Money.of("1200.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("700.00"));
        assertThat(saved.getStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
        assertThat(saved.getClosedDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(saved.getClosed()).isTrue();

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.description()).isEqualTo(saved.getDescription());
        assertThat(output.budget()).isEqualByComparingTo("1200.00");
        assertThat(output.remaining()).isEqualByComparingTo("700.00");
        assertThat(output.startDate()).isEqualTo(saved.getStartDate());
        assertThat(output.closedDate()).isEqualTo(saved.getClosedDate());
        assertThat(output.isClosed()).isEqualTo(saved.getClosed());
    }

    @Test
    void execute_whenWalletDoesNotExist_throwsAndDoesNotSave() {
        PatchWalletInput input = new PatchWalletInput(
                "wallet-missing",
                "may",
                null,
                null,
                null,
                null,
                null
        );
        when(walletRepository.findById("wallet-missing", "legacy")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(WalletNotFoundException.class)
                .hasMessageContaining("wallet-missing");

        verify(walletRepository, never()).save(any());
    }

    @Test
    void execute_whenPatchIsEmpty_savesCurrentStateUnchanged() {
        Wallet existing = new Wallet(
                "wallet-1",
                "monthly",
                Money.of("1000.00"),
                Money.of("700.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                false,
                YearMonth.of(2026, 4),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
        PatchWalletInput input = new PatchWalletInput(
                "wallet-1",
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(walletRepository.findById("wallet-1", "legacy")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }

    @Test
    void execute_futureWalletOutput_deductsActiveInstallment() {
        Wallet existing = new Wallet(
                "wallet-1",
                "june",
                Money.of("3000.00"),
                Money.of("3000.00"),
                LocalDate.of(2026, 6, 1),
                null,
                false,
                YearMonth.of(2026, 6),
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
        PatchWalletInput input = new PatchWalletInput(
                "wallet-1",
                "june",
                null,
                null,
                null,
                null,
                null
        );
        when(walletRepository.findById("wallet-1", "legacy")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepository.findActiveAffecting(YearMonth.of(2026, 6), "legacy"))
                .thenReturn(List.of(installment("1000.00", YearMonth.of(2026, 5))));

        WalletOutput output = useCase.execute(input);

        assertThat(output.remaining()).isEqualByComparingTo("2000.00");
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
