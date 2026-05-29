package br.com.casellisoftware.budgetmanager.rest.subscription.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.rest.validation.IsoCurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SubscriptionRequestDto(
        @NotBlank(message = "description is required")
        String description,

        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        @IsoCurrencyCode
        String currency,

        YearMonth effectiveMonth,

        @Pattern(regexp = "(?i)PRODUCTION|PREVIEW", message = "state must be PRODUCTION or PREVIEW")
        String state,
        FlagEnum flag,

        @NotBlank(message = "creditCardId is required")
        String creditCardId
) {
    public SubscriptionRequestDto(String description, BigDecimal amount, String currency, String creditCardId) {
        this(description, amount, currency, null, null, FlagEnum.NONE, creditCardId);
    }

    public SubscriptionRequestDto(String description, BigDecimal amount, String currency, YearMonth effectiveMonth, String state, String creditCardId) {
        this(description, amount, currency, effectiveMonth, state, FlagEnum.NONE, creditCardId);
    }
}
