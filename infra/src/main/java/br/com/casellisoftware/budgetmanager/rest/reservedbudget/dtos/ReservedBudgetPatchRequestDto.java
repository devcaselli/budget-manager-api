package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

public record ReservedBudgetPatchRequestDto(
        @Pattern(regexp = ".*\\S.*", message = "description must not be blank")
        String description,

        String details,

        @Positive(message = "newAmount must be positive")
        @Digits(integer = 12, fraction = 2, message = "newAmount must have at most 12 integer and 2 fraction digits")
        BigDecimal newAmount,

        FlagEnum flag,

        /**
         * Month from which a {@code newAmount} takes effect. When omitted, the use case
         * falls back to the current month (clock). Must not predate {@code startMonth} —
         * the domain rejects earlier months.
         */
        YearMonth effectiveMonth
) {
    public ReservedBudgetPatchRequestDto(String description, BigDecimal newAmount) {
        this(description, null, newAmount, FlagEnum.NONE, null);
    }
}
