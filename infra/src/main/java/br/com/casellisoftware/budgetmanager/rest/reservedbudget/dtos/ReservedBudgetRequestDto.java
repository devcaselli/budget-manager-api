package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.rest.validation.IsoCurrencyCode;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ReservedBudgetRequestDto(
        @NotBlank(message = "description is required")
        String description,

        String details,

        @NotNull(message = "budget is required")
        @Positive(message = "budget must be positive")
        @Digits(integer = 12, fraction = 2, message = "budget must have at most 12 integer and 2 fraction digits")
        BigDecimal budget,

        @NotBlank(message = "currency is required")
        @IsoCurrencyCode
        String currency,

        @NotNull(message = "effectiveMonth is required")
        YearMonth effectiveMonth,

        FlagEnum flag
) {
    public ReservedBudgetRequestDto(String description, String details, BigDecimal budget, String currency, YearMonth effectiveMonth) {
        this(description, details, budget, currency, effectiveMonth, FlagEnum.NONE);
    }
}
