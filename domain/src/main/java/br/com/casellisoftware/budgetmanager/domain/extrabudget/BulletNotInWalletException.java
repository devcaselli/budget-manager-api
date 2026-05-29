package br.com.casellisoftware.budgetmanager.domain.extrabudget;

public class BulletNotInWalletException extends RuntimeException {
    public BulletNotInWalletException(String bulletId, String walletId) {
        super("Bullet " + bulletId + " does not belong to wallet " + walletId);
    }
}
