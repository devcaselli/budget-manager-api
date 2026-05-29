package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerInput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavePayerUseCaseTest {

    @Mock
    private PayerRepository payerRepository;

    @Mock
    private PayerAmountDueCalculator calculator;

    private SavePayerUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SavePayerUseCase(payerRepository, calculator);
    }

    @Test
    void execute_createsPersistsAndReturnsOutputWithAmountDue() {
        when(payerRepository.save(any(Payer.class))).thenAnswer(inv -> inv.getArgument(0));
        when(calculator.calculate(any(Payer.class), any())).thenReturn(new PayerAmountDue(Money.zero(), Money.zero()));

        PayerOutput output = useCase.execute(new PayerInput(
                "Joao",
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                "owner-1"));

        ArgumentCaptor<Payer> captor = ArgumentCaptor.forClass(Payer.class);
        verify(payerRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo("owner-1");
        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(output.name()).isEqualTo("Joao");
        assertThat(output.amountDue()).isEqualTo(Money.zero());
    }
}
