package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Input DTO for the patch-installment use case. All fields except {@code id} are
 * nullable — only non-null fields will be applied to the existing entity.
 *
 * <p>Constraint: {@code originalValue} and {@code installmentValue} are mutually exclusive.</p>
 */
public record PatchInstallmentInput(
        String id,
        String details,
        FlagEnum flag,
        BigDecimal originalValue,
        BigDecimal installmentValue,
        Integer installmentNumber,
        YearMonth sourceEffectiveMonth,
        LocalDate purchaseDate,
        String creditCardId,
        String ownerId
) {
    public PatchInstallmentInput withOwnerId(String ownerId) {
        return new PatchInstallmentInput(id, details, flag, originalValue, installmentValue,
                installmentNumber, sourceEffectiveMonth, purchaseDate, creditCardId, ownerId);
    }
}
