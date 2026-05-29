package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;

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
                payment.getBulletId(),
                payment.getFlag(),
                payment.getKind(),
                payment.getPayerId(),
                payment.getShareId(),
                payment.isReversal(),
                payment.getReversedPaymentId()
        );
    }
}
