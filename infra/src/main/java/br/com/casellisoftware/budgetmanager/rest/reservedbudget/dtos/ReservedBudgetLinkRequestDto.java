package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.YearMonth;

public record ReservedBudgetLinkRequestDto(
        @NotNull ReservedBudgetLinkSourceType sourceType,
        @NotBlank String sourceId,
        @NotNull YearMonth fromMonth
) {
}
