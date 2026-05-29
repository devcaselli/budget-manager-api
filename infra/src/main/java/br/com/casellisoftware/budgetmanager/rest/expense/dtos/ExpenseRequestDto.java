package br.com.casellisoftware.budgetmanager.rest.expense.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNumberPolicy;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * HTTP boundary DTO for the save-expense endpoint. Bean Validation here is the
 * first line of defense: it rejects malformed payloads at the edge with
 * per-field messages, before anything reaches the use case or the domain.
 *
 * <p>The domain entity ({@code Expense}) still enforces the same invariants
 * independently — this is intentional defense in depth: future non-HTTP entry
 * points (CLI, queue, scheduler) won't pass through this DTO.</p>
 */
public record ExpenseRequestDto(

        @NotBlank
        @Size(max = 120)
        String name,

        @NotNull
        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal cost,

        @NotNull
        @PastOrPresent
        LocalDate purchaseDate,

        @NotBlank
        String walletId,

        @NotBlank
        String creditCardId,

        Boolean installment,

        @Min(InstallmentNumberPolicy.MIN_INSTALLMENTS)
        @Max(InstallmentNumberPolicy.MAX_INSTALLMENTS)
        Integer installmentNumber,

        FlagEnum flag
) {
    @AssertTrue(message = "installmentNumber is required when installment is true")
    public boolean isInstallmentNumberPresentWhenFlagged() {
        if (Boolean.TRUE.equals(installment)) {
            return installmentNumber != null
                    && installmentNumber >= InstallmentNumberPolicy.MIN_INSTALLMENTS;
        }
        return true;
    }
}
