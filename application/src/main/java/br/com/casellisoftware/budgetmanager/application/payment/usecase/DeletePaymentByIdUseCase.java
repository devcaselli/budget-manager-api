package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeletePaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

public class DeletePaymentByIdUseCase implements DeletePaymentByIdBoundary {

    private final PaymentRepository paymentRepository;

    public DeletePaymentByIdUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public void execute(String id) {
        this.paymentRepository.deleteById(id);
    }
}
