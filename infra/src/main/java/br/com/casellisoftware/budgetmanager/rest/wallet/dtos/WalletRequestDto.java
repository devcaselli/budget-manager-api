package br.com.casellisoftware.budgetmanager.rest.wallet.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record WalletRequestDto(

         String description,
         @NotNull
         @Positive
         BigDecimal budget,
         @NotNull
         LocalDate startDate,
         @Future
         LocalDate closedDate,
         Boolean closed,
         @NotNull
         YearMonth effectiveMonth,
         WalletState state,
         FlagEnum flag
) {
    public WalletRequestDto(String description,
                            BigDecimal budget,
                            LocalDate startDate,
                            LocalDate closedDate,
                            Boolean closed,
                            YearMonth effectiveMonth,
                            WalletState state) {
        this(description, budget, startDate, closedDate, closed, effectiveMonth, state, FlagEnum.NONE);
    }
}
