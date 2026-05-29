package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;

/**
 * Input DTO for the save-bullet use case. Carries raw primitive types from the
 * interface adapter layer; canonical validation happens when the use case calls
 * {@code Bullet.create(...)}.
 */
public record BulletInput(
        String description,
        BigDecimal budget,
        String walletId,
        FlagEnum flag,
        String ownerId
) {
    public BulletInput(String description, BigDecimal budget, String walletId, FlagEnum flag) {
        this(description, budget, walletId, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public BulletInput(String description, BigDecimal budget, String walletId) {
        this(description, budget, walletId, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public BulletInput withOwnerId(String ownerId) {
        return new BulletInput(description, budget, walletId, flag, ownerId);
    }
}
