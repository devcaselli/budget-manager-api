package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeletePaymentByIdUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private DeletePaymentByIdUseCase useCase;

    @Test
    void execute_deletesByIdWithoutPreLookup() {
        useCase.execute("payment-1");

        verify(paymentRepository).deleteById("payment-1");
        verifyNoMoreInteractions(paymentRepository);
    }
}
