package br.com.casellisoftware.budgetmanager.rest.installment.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record InstallmentPatchRequestDto(

        @Size(max = 500)
        String details,

        FlagEnum flag,

        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal originalValue,

        @Positive
        @Digits(integer = 12, fraction = 2)
        BigDecimal installmentValue,

        @Min(2)
        Integer installmentNumber,

        YearMonth sourceEffectiveMonth,

        LocalDate purchaseDate,

        String creditCardId
) {
}
