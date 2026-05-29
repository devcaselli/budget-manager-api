package br.com.casellisoftware.budgetmanager.application.wallet.boundary;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

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
        Boolean isClosed,
        YearMonth effectiveMonth,
        WalletState state,
        FlagEnum flag
) {
    public WalletOutput(String id,
                        String description,
                        BigDecimal budget,
                        BigDecimal remaining,
                        LocalDate startDate,
                        LocalDate closedDate,
                        Boolean isClosed,
                        YearMonth effectiveMonth,
                        WalletState state) {
        this(id, description, budget, remaining, startDate, closedDate, isClosed, effectiveMonth, state, FlagEnum.NONE);
    }
}
