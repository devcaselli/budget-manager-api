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
class FindWalletPayersUseCaseTest {

    @Mock
    private PayerRepository payerRepository;

    @Mock
    private PayerAmountDueCalculator calculator;

    private FindWalletPayersUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindWalletPayersUseCase(payerRepository, calculator);
    }

    @Test
    void execute_returnsStandingPlusWalletTransientPayers() {
        Payer standing = new Payer("payer-1", "owner-1", "Standing", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), false);
        Payer transientPayer = new Payer("payer-2", "owner-1", "Transient", PayerType.TRANSIENT, "wallet-1", null, LocalDate.of(2026, 5, 10), false);
        when(payerRepository.findAllStanding("owner-1")).thenReturn(List.of(standing));
        when(payerRepository.findAllByWalletId("wallet-1", "owner-1")).thenReturn(List.of(transientPayer));
        when(calculator.calculate(any(Payer.class), any())).thenReturn(new PayerAmountDue(Money.zero(), Money.zero()));

        List<PayerOutput> outputs = useCase.execute("wallet-1", "owner-1");

        assertThat(outputs).hasSize(2);
        assertThat(outputs).extracting(PayerOutput::id).containsExactly("payer-1", "payer-2");
    }
}
