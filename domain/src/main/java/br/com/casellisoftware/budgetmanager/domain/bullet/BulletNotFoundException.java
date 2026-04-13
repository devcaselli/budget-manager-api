package br.com.casellisoftware.budgetmanager.domain.bullet;

public class BulletNotFoundException extends RuntimeException {
    public BulletNotFoundException(String message) {
        super(message);
    }
}
