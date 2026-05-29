package br.com.casellisoftware.budgetmanager.rest.subscription.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SubscriptionPatchRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "description must not be blank")
        String description,

        @Positive(message = "newAmount must be positive")
        BigDecimal newAmount,

        @NotBlank(message = "creditCardId must not be blank")
        String creditCardId,

        FlagEnum flag
) {
    public SubscriptionPatchRequestDto(String description, BigDecimal newAmount) {
        this(description, newAmount, null, FlagEnum.NONE);
    }

    public SubscriptionPatchRequestDto(String description, BigDecimal newAmount, String creditCardId) {
        this(description, newAmount, creditCardId, FlagEnum.NONE);
    }
}
