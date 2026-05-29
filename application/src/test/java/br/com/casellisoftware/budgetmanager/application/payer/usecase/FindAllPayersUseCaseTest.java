package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.domain.payer.Payer;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAllPayersUseCaseTest {

    @Mock
    private PayerRepository payerRepository;

    @Mock
    private PayerAmountDueCalculator calculator;

    private FindAllPayersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindAllPayersUseCase(payerRepository, calculator);
    }

    @Test
    void execute_returnsAllMappedOutputsWithAmountDue() {
        Payer first = payer("payer-1", "Joao");
        Payer second = payer("payer-2", "Maria");
        when(payerRepository.findAllStanding("owner-1")).thenReturn(List.of(first, second));
        when(calculator.calculate(any(Payer.class), any())).thenReturn(
                new PayerAmountDue(Money.of("10.00"), Money.of("10.00")),
                new PayerAmountDue(Money.of("20.00"), Money.of("20.00")));

        List<PayerOutput> outputs = useCase.execute("owner-1");

        assertThat(outputs).extracting(PayerOutput::id).containsExactly("payer-1", "payer-2");
        assertThat(outputs).extracting(output -> output.amountDue().amount())
                .containsExactly(Money.of("10.00").amount(), Money.of("20.00").amount());
    }

    private static Payer payer(String id, String name) {
        return new Payer(id, "owner-1", name, PayerType.STANDING, null, LocalDate.of(2026, 5, 10), false);
    }
}
