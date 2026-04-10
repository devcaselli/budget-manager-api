package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input DTO for the save-wallet use case. Carries raw primitive types from the
 * interface adapter layer; canonical validation happens when the use case calls
 * {@code Wallet.create(...)}.
 */
public record WalletInput(
        String description,
        BigDecimal budget,
        LocalDate startDate,
        LocalDate closedDate,
        Boolean isClosed
) {
}