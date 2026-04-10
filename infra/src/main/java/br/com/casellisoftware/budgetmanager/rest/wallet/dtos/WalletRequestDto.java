package br.com.casellisoftware.budgetmanager.rest.wallet.dtos;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WalletRequestDto(

         String description,
         @NotNull
         @Positive
         BigDecimal budget,
         @NotNull
         LocalDate startDate,
         @Future
         LocalDate closedDate,
         Boolean closed
) {
}
