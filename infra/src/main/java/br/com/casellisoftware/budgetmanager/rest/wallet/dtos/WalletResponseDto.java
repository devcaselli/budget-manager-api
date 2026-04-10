package br.com.casellisoftware.budgetmanager.rest.wallet.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WalletResponseDto(

         String id,
         String description,
         BigDecimal budget,
         BigDecimal remaining,
         LocalDate startDate,
         LocalDate closedDate,
         Boolean closed
) {
}
