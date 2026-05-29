package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record InstallmentOutput(
        String id,
        String description,
        String details,
        BigDecimal originalValue,
        BigDecimal installmentValue,
        String currency,
        int installmentNumber,
        LocalDate purchaseDate,
        YearMonth lastInstallmentDate,
        String creditCardId,
        String sourceWalletId,
        YearMonth sourceEffectiveMonth,
        boolean deleted,
        LocalDateTime deletedAt,
        FlagEnum flag,
        boolean shared,
        BigDecimal ownerRatio,
        BigDecimal effectiveOriginalValue,
        BigDecimal effectiveInstallmentValue
) {
    public InstallmentOutput(String id,
                             String description,
                             String details,
                             BigDecimal originalValue,
                             BigDecimal installmentValue,
                             String currency,
                             int installmentNumber,
                             LocalDate purchaseDate,
                             YearMonth lastInstallmentDate,
                             String creditCardId,
                             String sourceWalletId,
                             YearMonth sourceEffectiveMonth,
                             boolean deleted,
                             LocalDateTime deletedAt,
                             FlagEnum flag) {
        this(id, description, details, originalValue, installmentValue, currency,
                installmentNumber, purchaseDate, lastInstallmentDate, creditCardId,
                sourceWalletId, sourceEffectiveMonth, deleted, deletedAt, flag,
                false, null, originalValue, installmentValue);
    }
}
