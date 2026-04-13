package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Input DTO for the patch-wallet use case. All fields are nullable — only
 * non-null fields will be applied to the existing entity.
 * The {@code id} is required to identify which wallet to patch.
 */
public record PatchWalletInput(
        String id,
        String description,
        BigDecimal budget,
        LocalDate startDate,
        LocalDate closedDate,
        Boolean closed
) {
}
