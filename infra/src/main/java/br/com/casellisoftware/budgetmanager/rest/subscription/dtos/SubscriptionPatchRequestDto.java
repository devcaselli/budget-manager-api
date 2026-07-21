package br.com.casellisoftware.budgetmanager.rest.subscription.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SubscriptionPatchRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "description must not be blank")
        String description,

        @Positive(message = "newAmount must be positive")
        BigDecimal newAmount,

        @NotBlank(message = "creditCardId must not be blank")
        String creditCardId,

        FlagEnum flag,

        /**
         * Month from which a {@code newAmount} takes effect. When omitted, the use case
         * falls back to the current month (clock). Must not predate {@code startMonth} —
         * the domain rejects earlier months.
         */
        YearMonth effectiveMonth
) {
    public SubscriptionPatchRequestDto(String description, BigDecimal newAmount) {
        this(description, newAmount, null, FlagEnum.NONE, null);
    }

    public SubscriptionPatchRequestDto(String description, BigDecimal newAmount, String creditCardId) {
        this(description, newAmount, creditCardId, FlagEnum.NONE, null);
    }
}
