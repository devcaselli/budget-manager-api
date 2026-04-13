package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import java.math.BigDecimal;

/**
 * Input DTO for the patch-bullet use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 */
public record PatchBulletInput(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining
) {
}
