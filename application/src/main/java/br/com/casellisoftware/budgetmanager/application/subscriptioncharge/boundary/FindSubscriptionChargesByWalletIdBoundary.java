package br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary;

import java.util.List;

public interface FindSubscriptionChargesByWalletIdBoundary {

    List<SubscriptionChargeOutput> execute(String walletId, String ownerId);
}
