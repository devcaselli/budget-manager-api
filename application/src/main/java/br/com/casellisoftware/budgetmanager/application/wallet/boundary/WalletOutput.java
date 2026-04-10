package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Output DTO for wallet operations.
 */
public record WalletOutput(
        String id,
        String description,
        BigDecimal budget,
        BigDecimal remaining,
        LocalDate startDate,
        LocalDate closedDate,
        Boolean isClosed
) {
}