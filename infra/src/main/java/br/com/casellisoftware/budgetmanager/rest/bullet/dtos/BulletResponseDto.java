package br.com.casellisoftware.budgetmanager.rest.bullet.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import java.math.BigDecimal;

public record BulletResponseDto(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining,
        String walletId,
        FlagEnum flag
) {
    public BulletResponseDto(String id, String description, BigDecimal budget, BigDecimal remaining, String walletId) {
        this(id, description, budget, remaining, walletId, FlagEnum.NONE);
    }
}
