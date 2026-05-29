package br.com.casellisoftware.budgetmanager.rest.installment.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Request body for creating a standalone installment (not linked to a wallet expense).
 *
 * <p>Exactly one of {@code originalValue} or {@code installmentValue} must be provided.
 * This mutual-exclusion constraint is validated in the use case, not at DTO level.</p>
 */
public record SaveStandaloneInstallmentRequestDto(
        @NotBlank String description,
        String details,
        BigDecimal originalValue,
        BigDecimal installmentValue,
        @NotBlank String currency,
        @Min(2) @Max(120) int installmentNumber,
        @NotNull LocalDate purchaseDate,
        @NotBlank String creditCardId,
        @NotNull YearMonth sourceEffectiveMonth,
        FlagEnum flag
) {
}
