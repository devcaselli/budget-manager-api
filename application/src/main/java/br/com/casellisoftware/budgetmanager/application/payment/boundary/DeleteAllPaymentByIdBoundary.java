package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import java.util.List;

public interface DeleteAllPaymentByIdBoundary {

    void execute(List<String> ids);
}
