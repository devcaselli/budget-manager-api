package br.com.casellisoftware.budgetmanager.domain.subscription.exception;

import java.time.YearMonth;

/**
 * Thrown when an {@code endMonth} value is strictly before the subscription's
 * {@code startMonth}. Extends {@link IllegalArgumentException} so that existing
 * handlers that catch the broader type continue to work.
 */
public class EndMonthBeforeStartMonthException extends IllegalArgumentException {

    public EndMonthBeforeStartMonthException(YearMonth startMonth, YearMonth endMonth) {
        super("endMonth must not be before startMonth: endMonth=" + endMonth + ", startMonth=" + startMonth);
    }
}
