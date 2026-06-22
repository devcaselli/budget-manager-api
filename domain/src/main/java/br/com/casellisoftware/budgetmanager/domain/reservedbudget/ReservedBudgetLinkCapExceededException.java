package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.YearMonth;

/**
 * Thrown when linking or editing a {@link ReservedBudget} would cause the sum of all linked
 * item amounts to exceed the reserved budget ceiling in at least one month.
 */
public class ReservedBudgetLinkCapExceededException extends RuntimeException {

    private final YearMonth month;
    private final Money sum;
    private final Money ceiling;

    public ReservedBudgetLinkCapExceededException(YearMonth month, Money sum, Money ceiling) {
        super(String.format(
                "Reserved budget cap exceeded in %s: sum %s > ceiling %s",
                month, sum.amount(), ceiling.amount()));
        this.month = month;
        this.sum = sum;
        this.ceiling = ceiling;
    }

    public YearMonth getMonth() {
        return month;
    }

    public Money getSum() {
        return sum;
    }

    public Money getCeiling() {
        return ceiling;
    }
}
