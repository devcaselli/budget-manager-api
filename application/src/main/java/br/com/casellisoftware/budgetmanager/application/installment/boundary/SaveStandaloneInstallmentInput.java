package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record SaveStandaloneInstallmentInput(
        String description,
        String details,
        BigDecimal originalValue,
        BigDecimal installmentValue,
        String currency,
        int installmentNumber,
        LocalDate purchaseDate,
        String creditCardId,
        YearMonth sourceEffectiveMonth,
        FlagEnum flag,
        String ownerId
) {
}
