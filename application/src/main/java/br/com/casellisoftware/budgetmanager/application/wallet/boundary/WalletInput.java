package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

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
        Boolean isClosed,
        YearMonth effectiveMonth,
        WalletState state,
        FlagEnum flag,
        String ownerId
) {
    public WalletInput(String description,
                       BigDecimal budget,
                       LocalDate startDate,
                       LocalDate closedDate,
                       Boolean isClosed,
                       YearMonth effectiveMonth,
                       WalletState state,
                       FlagEnum flag) {
        this(description, budget, startDate, closedDate, isClosed, effectiveMonth, state, flag, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public WalletInput(String description,
                       BigDecimal budget,
                       LocalDate startDate,
                       LocalDate closedDate,
                       Boolean isClosed,
                       YearMonth effectiveMonth,
                       WalletState state) {
        this(description, budget, startDate, closedDate, isClosed, effectiveMonth, state, FlagEnum.NONE, AuthenticatedUser.LEGACY_OWNER_ID);
    }

    public WalletInput withOwnerId(String ownerId) {
        return new WalletInput(description, budget, startDate, closedDate, isClosed, effectiveMonth, state, flag, ownerId);
    }
}
