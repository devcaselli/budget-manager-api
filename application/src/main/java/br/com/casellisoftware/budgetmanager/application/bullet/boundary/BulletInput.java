package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import java.math.BigDecimal;

/**
 * Input DTO for the save-bullet use case. Carries raw primitive types from the
 * interface adapter layer; canonical validation happens when the use case calls
 * {@code Bullet.create(...)}.
 */
public record BulletInput(
        String description,
        BigDecimal budget,
        String walletId
) {
}
