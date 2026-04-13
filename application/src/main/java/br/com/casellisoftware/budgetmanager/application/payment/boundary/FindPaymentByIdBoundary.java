package br.com.casellisoftware.budgetmanager.application.payment.boundary;

public interface FindPaymentByIdBoundary {

    PaymentOutput execute(String id);
}
