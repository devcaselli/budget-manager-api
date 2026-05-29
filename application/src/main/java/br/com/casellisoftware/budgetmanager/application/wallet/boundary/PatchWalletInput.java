package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

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
        Boolean closed,
        WalletState state,
        FlagEnum flag,
        String ownerId
) {
    public PatchWalletInput(String id,
                            String description,
                            BigDecimal budget,
                            LocalDate startDate,
                            LocalDate closedDate,
                            Boolean closed,
                            WalletState state,
                            FlagEnum flag) {
        this(id, description, budget, startDate, closedDate, closed, state, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchWalletInput(String id,
                            String description,
                            BigDecimal budget,
                            LocalDate startDate,
                            LocalDate closedDate,
                            Boolean closed,
                            WalletState state) {
        this(id, description, budget, startDate, closedDate, closed, state, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public PatchWalletInput withOwnerId(String ownerId) {
        return new PatchWalletInput(id, description, budget, startDate, closedDate, closed, state, flag, ownerId);
    }
}
