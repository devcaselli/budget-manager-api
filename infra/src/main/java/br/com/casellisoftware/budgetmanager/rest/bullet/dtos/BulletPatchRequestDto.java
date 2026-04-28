package br.com.casellisoftware.budgetmanager.rest.bullet.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BulletPatchRequestDto(

        @Size(max = 120)
        String description,

        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal budget,

        @PositiveOrZero
        @Digits(integer = 12, fraction = 2)
        BigDecimal remaining,

        String walletId
) {
}
