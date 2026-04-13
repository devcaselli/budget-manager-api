package br.com.casellisoftware.budgetmanager.rest.bullet.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * HTTP boundary DTO for the save-bullet endpoint. Bean Validation here is the
 * first line of defense: it rejects malformed payloads at the edge with
 * per-field messages, before anything reaches the use case or the domain.
 */
public record BulletRequestDto(

        @NotBlank
        String description,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal budget,

        @NotBlank
        String walletId
) {
}
