package br.com.casellisoftware.budgetmanager.application.bullet.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;

public record BulletOutput(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining,
        String walletId,
        FlagEnum flag
) {
    public BulletOutput(String id, String description, BigDecimal budget, BigDecimal remaining, String walletId) {
        this(id, description, budget, remaining, walletId, FlagEnum.NONE);
    }
}
