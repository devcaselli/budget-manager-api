package br.com.casellisoftware.budgetmanager.application.wallet.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchWalletUseCaseTest {

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private PatchWalletUseCase useCase;

    @Test
    void execute_patchesOnlyPatchableFields() {
        Wallet existing = new Wallet(
                "wallet-1",
                "monthly",
                Money.of("1000.00"),
                Money.of("700.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                false
        );
        PatchWalletInput input = new PatchWalletInput(
                "wallet-1",
                "may",
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 31),
                true
        );

        when(walletRepository.findById("wallet-1")).thenReturn(Optional.of(existing));
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
                null
        );
        when(walletRepository.findById("wallet-missing")).thenReturn(Optional.empty());

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
                false
        );
        PatchWalletInput input = new PatchWalletInput(
                "wallet-1",
                null,
                null,
                null,
                null,
                null
        );

        when(walletRepository.findById("wallet-1")).thenReturn(Optional.of(existing));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Wallet> captor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
    }
}
