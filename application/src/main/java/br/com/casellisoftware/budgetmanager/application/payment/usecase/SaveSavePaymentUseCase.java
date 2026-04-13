package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.SavePaymentBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

import java.time.Instant;

public class SaveSavePaymentUseCase implements SavePaymentBoundary {

    private final PaymentRepository paymentRepository;

    public SaveSavePaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public PaymentOutput execute(PaymentInput input) {

        Payment payment = Payment.create(
                input.amount(),
                Instant.now(),
                input.details(),
                input.expenseId(),
                input.walletId()
        );

        Payment saved = paymentRepository.save(payment);

        return PaymentOutputAssembler.from(saved);
    }
}
