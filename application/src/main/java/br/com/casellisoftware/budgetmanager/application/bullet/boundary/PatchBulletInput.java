package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

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
        String walletId
) {
}
