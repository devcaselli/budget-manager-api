package br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * HTTP boundary DTO for the save-extra-budget endpoint. Bean Validation here is
 * the first line of defense, rejecting malformed payloads at the edge before
 * anything reaches the use case or domain layer.
 */
public record ExtraBudgetRequestDto(

        @NotBlank
        String description,

        @NotBlank
        String walletId,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal amount,

        @NotEmpty
        @Valid
        List<AllocationRequestDto> allocations
) {
}
