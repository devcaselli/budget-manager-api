package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

import java.util.List;

public class DeleteAllPaymentByIdUseCase implements DeleteAllPaymentByIdBoundary {

    private final PaymentRepository paymentRepository;

    public DeleteAllPaymentByIdUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }


    @Override
    public void execute(List<String> ids) {
        this.paymentRepository.deleteAllById(ids);
    }
}
