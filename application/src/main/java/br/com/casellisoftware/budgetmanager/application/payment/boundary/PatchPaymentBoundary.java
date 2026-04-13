package br.com.casellisoftware.budgetmanager.application.payment.boundary;

public interface PatchPaymentBoundary {

    PaymentOutput execute(PatchPaymentInput input);
}
