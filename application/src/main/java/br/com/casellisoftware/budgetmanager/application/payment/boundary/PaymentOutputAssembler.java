package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;

public class PaymentOutputAssembler {

    private PaymentOutputAssembler() {}

    public static PaymentOutput from(Payment payment){
        return new PaymentOutput(
                payment.getId(),
                payment.getAmount(),
                payment.getPaymentDate(),
                payment.getDetails(),
                payment.getExpenseId(),
                payment.getWalletId(),
                payment.getBulletId()
        );
    }
}
