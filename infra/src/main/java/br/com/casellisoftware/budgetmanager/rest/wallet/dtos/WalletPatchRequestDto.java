package br.com.casellisoftware.budgetmanager.rest.wallet.dtos;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for {@code PATCH /wallets/{id}}. All fields are nullable; only
 * non-null fields are applied.
 */
public record WalletPatchRequestDto(
        String description,
        BigDecimal budget,
        LocalDate closedDate,
        Boolean closed,
        WalletState state,
        FlagEnum flag
) {
    public WalletPatchRequestDto(String description,
                                 BigDecimal budget,
                                 LocalDate closedDate,
                                 Boolean closed,
                                 WalletState state) {
        this(description, budget, closedDate, closed, state, FlagEnum.NONE);
    }
}
