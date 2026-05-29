package br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * HTTP boundary DTO for a single allocation within an {@link ExtraBudgetRequestDto}.
 */
public record AllocationRequestDto(

        @NotBlank
        String bulletId,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount
) {
}
