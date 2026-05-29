package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;

/**
 * Input DTO for the patch-bullet use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 *
 * <p>{@code walletId} is accepted for contract completeness, but a bullet
 * cannot be reassigned to a different wallet via patch. When provided, the
 * value must match the current wallet id.</p>
 */
public record PatchBulletInput(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining,
        String walletId,
        FlagEnum flag,
        String ownerId
) {
    public PatchBulletInput(String id, String description, BigDecimal budget, BigDecimal remaining, String walletId, FlagEnum flag) {
        this(id, description, budget, remaining, walletId, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchBulletInput(String id, String description, BigDecimal budget, BigDecimal remaining, String walletId) {
        this(id, description, budget, remaining, walletId, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchBulletInput withOwnerId(String ownerId) {
        return new PatchBulletInput(id, description, budget, remaining, walletId, flag, ownerId);
    }
}
