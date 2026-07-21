package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PluggyTransactionPreviewResponseDto(
        String id,
        String accountId,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate date,
        boolean isExpense,
        boolean alreadyImported
) {
}
