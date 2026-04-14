package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import java.util.List;

public interface FindAllPaymentByExpenseIdBoundary {

    List<PaymentOutput> execute(String expenseId);
}
