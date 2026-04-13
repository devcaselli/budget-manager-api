package br.com.casellisoftware.budgetmanager.rest.bullet.dtos;

import java.math.BigDecimal;

public record BulletResponseDto(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining,
        String walletId
) {
}
