package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

import java.util.List;

public class FindAllPaymentByExpenseIdUseCase {

    private final PaymentRepository paymentRepository;

    public FindAllPaymentByExpenseIdUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<PaymentOutput> execute(String expenseId){
        return this.paymentRepository.findAllByExpenseId(expenseId)
                .stream()
                .map(PaymentOutputAssembler::from)
                .toList();
    }
}
