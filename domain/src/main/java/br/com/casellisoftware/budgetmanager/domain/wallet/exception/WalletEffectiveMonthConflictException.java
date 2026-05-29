package br.com.casellisoftware.budgetmanager.domain.wallet.exception;

import java.time.YearMonth;

/**
 * Thrown when an attempt is made to create or transition a wallet to
 * {@code PRODUCTION} state for an {@code effectiveMonth} that already has
 * another open PRODUCTION wallet.
 */
public class WalletEffectiveMonthConflictException extends RuntimeException {

    public WalletEffectiveMonthConflictException(YearMonth effectiveMonth) {
        super("an open PRODUCTION wallet already exists for effectiveMonth=" + effectiveMonth);
    }

    public WalletEffectiveMonthConflictException(String message) {
        super(message);
    }
}
