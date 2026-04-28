package br.com.casellisoftware.budgetmanager.domain.bullet;

public class BulletInUseException extends RuntimeException {

    public BulletInUseException(String id) {
        super("Bullet is in use by payments: " + id);
    }
}
