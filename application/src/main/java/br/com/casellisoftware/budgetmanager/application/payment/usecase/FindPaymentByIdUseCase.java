package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

public class FindPaymentByIdUseCase implements FindPaymentByIdBoundary {

    private final PaymentRepository paymentRepository;

    public FindPaymentByIdUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }


    @Override
    public PaymentOutput execute(String id) {
        Payment payment = this.paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found, id: " + id));

        return PaymentOutputAssembler.from(payment);
    }
}
