package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPayerByIdUseCaseTest {

    @Mock
    private PayerRepository payerRepository;

    @Mock
    private PayerAmountDueCalculator calculator;

    private FindPayerByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindPayerByIdUseCase(payerRepository, calculator);
    }

    @Test
    void findById_whenFound_returnsOutput() {
        Payer payer = new Payer("payer-1", "owner-1", "Joao", PayerType.STANDING, null, LocalDate.of(2026, 5, 10), false);
        when(payerRepository.findById("payer-1", "owner-1")).thenReturn(Optional.of(payer));
        when(calculator.calculate(any(Payer.class), any())).thenReturn(new PayerAmountDue(Money.of("25.00"), Money.of("25.00")));

        PayerOutput output = useCase.findById("payer-1", "owner-1");

        assertThat(output.id()).isEqualTo("payer-1");
        assertThat(output.amountDue().amount()).isEqualByComparingTo("25.00");
    }

    @Test
    void findById_whenMissing_throws() {
        when(payerRepository.findById("missing", "owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById("missing", "owner-1"))
                .isInstanceOf(PayerNotFoundException.class);
    }
}
