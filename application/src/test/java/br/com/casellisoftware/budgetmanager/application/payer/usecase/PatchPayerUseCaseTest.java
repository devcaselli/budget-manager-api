package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerPatchInput;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerLifecycleChangeNotAllowedException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchPayerUseCaseTest {

    @Mock
    private PayerRepository payerRepository;

    @Mock
    private PayerAmountDueCalculator calculator;

    @Mock
    private ShareRepository shareRepository;

    private PatchPayerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchPayerUseCase(payerRepository, calculator);
    }

    @Test
    void execute_whenFound_patchesAndReturnsOutput() {
        Payer payer = payer();
        when(payerRepository.findById("payer-1", "owner-1")).thenReturn(Optional.of(payer));
        when(payerRepository.save(any(Payer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(calculator.calculate(any(Payer.class), any())).thenReturn(new PayerAmountDue(Money.of("10.00"), Money.of("10.00")));

        PayerOutput output = useCase.execute(
                "payer-1",
                new PayerPatchInput(Optional.of("Maria"), Optional.of(PayerType.TRANSIENT), Optional.of("wallet-1"), Optional.empty(), Optional.empty()),
                "owner-1");

        assertThat(output.name()).isEqualTo("Maria");
        assertThat(output.type()).isEqualTo(PayerType.TRANSIENT);
        assertThat(output.walletId()).isEqualTo("wallet-1");
        assertThat(output.amountDue().amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void execute_whenMissing_throwsAndDoesNotSave() {
        when(payerRepository.findById("missing", "owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("missing", PayerPatchInput.empty(), "owner-1"))
                .isInstanceOf(PayerNotFoundException.class)
                .hasMessageContaining("missing");

        verify(payerRepository, never()).save(any());
    }

    @Test
    void execute_whenTypeChangesAndPayerIsUsedInShare_throwsConflict() {
        PatchPayerUseCase useCaseWithShare = new PatchPayerUseCase(payerRepository, calculator, shareRepository);
        Payer payer = payer();
        when(payerRepository.findById("payer-1", "owner-1")).thenReturn(Optional.of(payer));
        when(shareRepository.existsByPayerId("payer-1", "owner-1")).thenReturn(true);

        assertThatThrownBy(() -> useCaseWithShare.execute(
                "payer-1",
                new PayerPatchInput(Optional.empty(), Optional.of(PayerType.TRANSIENT), Optional.of("wallet-1"), Optional.empty(), Optional.empty()),
                "owner-1"))
                .isInstanceOf(PayerLifecycleChangeNotAllowedException.class);

        verify(payerRepository, never()).save(any());
    }

    private static Payer payer() {
        return new Payer(
                "payer-1",
                "owner-1",
                "Joao",
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                false);
    }
}
