package br.com.casellisoftware.budgetmanager.application.payment.boundary;

public interface PayExpenseBoundary {

    PaymentOutput execute(PayExpenseInput input);
}
