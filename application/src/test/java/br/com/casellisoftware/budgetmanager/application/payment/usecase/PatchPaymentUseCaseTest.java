package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PatchPaymentInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PatchPaymentUseCase useCase;

    @Test
    void execute_patchesOnlyPatchableFields() {
        Payment existing = Payment.rebuild(
                "payment-1",
                Money.of("25.00"),
                Instant.parse("2026-04-21T10:00:00Z"),
                "first",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );
        PatchPaymentInput input = new PatchPaymentInput(
                "payment-1",
                new BigDecimal("30.00"),
                "updated"
        );

        when(paymentRepository.findById("payment-1")).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentOutput output = useCase.execute(input);

        ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(captor.capture());
        Payment saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo("payment-1");
        assertThat(saved.getAmount()).isEqualTo(Money.of("30.00"));
        assertThat(saved.getDetails()).isEqualTo("updated");
        assertThat(saved.getPaymentDate()).isEqualTo(Instant.parse("2026-04-21T10:00:00Z"));
        assertThat(saved.getExpenseId()).isEqualTo("expense-1");
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        assertThat(saved.getBulletId()).isEqualTo("bullet-1");

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.amount()).isEqualTo(saved.getAmount());
        assertThat(output.details()).isEqualTo(saved.getDetails());
    }

    @Test
    void execute_whenPaymentDoesNotExist_throwsAndDoesNotSave() {
        PatchPaymentInput input = new PatchPaymentInput(
                "payment-missing",
                null,
                "updated"
        );
        when(paymentRepository.findById("payment-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(PaymentNotFoundException.class)
                .hasMessageContaining("payment-missing");

        verify(paymentRepository, never()).save(any());
    }
}
