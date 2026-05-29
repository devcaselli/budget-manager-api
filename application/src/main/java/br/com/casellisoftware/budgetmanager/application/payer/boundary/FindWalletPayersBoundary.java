package br.com.casellisoftware.budgetmanager.application.payer.boundary;

import java.util.List;

public interface FindWalletPayersBoundary {

    List<PayerOutput> execute(String walletId, String ownerId);
}
