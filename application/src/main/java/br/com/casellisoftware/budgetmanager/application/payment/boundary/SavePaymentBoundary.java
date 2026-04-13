package br.com.casellisoftware.budgetmanager.application.payment.boundary;

public interface SavePaymentBoundary {

    PaymentOutput execute(PaymentInput input);
}
