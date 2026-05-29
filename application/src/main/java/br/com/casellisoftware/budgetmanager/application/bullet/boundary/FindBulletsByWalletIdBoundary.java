package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import java.util.List;

public interface FindBulletsByWalletIdBoundary {

    List<BulletOutput> execute(String walletId, String ownerId);
}
