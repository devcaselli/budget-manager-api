package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

public interface FindPaymentsByWalletIdBoundary {

    PageResult<PaymentOutput> execute(String walletId, int page, int size, String ownerId);
}
