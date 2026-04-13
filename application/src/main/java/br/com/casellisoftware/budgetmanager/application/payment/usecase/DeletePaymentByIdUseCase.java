package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeletePaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

public class DeletePaymentByIdUseCase implements DeletePaymentByIdBoundary {

    private final PaymentRepository paymentRepository;
    private final FindPaymentByIdUseCase  findPaymentByIdUseCase;

    public DeletePaymentByIdUseCase(PaymentRepository paymentRepository, FindPaymentByIdUseCase findPaymentByIdUseCase) {
        this.paymentRepository = paymentRepository;
        this.findPaymentByIdUseCase = findPaymentByIdUseCase;
    }


    @Override
    public void execute(String id) {
        PaymentOutput execute = this.findPaymentByIdUseCase.execute(id);
        this.paymentRepository.deleteById(id);
    }
}
