package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import java.math.BigDecimal;

public record BulletOutput(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining,
        String walletId
) {
}
