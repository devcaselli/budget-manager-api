package br.com.casellisoftware.budgetmanager.rest.wallet.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record WalletResponseDto(

         String id,
         String description,
         BigDecimal budget,
         BigDecimal remaining,
         LocalDate startDate,
         LocalDate closedDate,
         Boolean closed,
         YearMonth effectiveMonth,
         WalletState state,
         FlagEnum flag
) {
    public WalletResponseDto(String id,
                             String description,
                             BigDecimal budget,
                             BigDecimal remaining,
                             LocalDate startDate,
                             LocalDate closedDate,
                             Boolean closed,
                             YearMonth effectiveMonth,
                             WalletState state) {
        this(id, description, budget, remaining, startDate, closedDate, closed, effectiveMonth, state, FlagEnum.NONE);
    }
}
