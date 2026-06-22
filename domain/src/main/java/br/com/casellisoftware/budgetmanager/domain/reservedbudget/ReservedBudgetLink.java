package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import java.time.YearMonth;
import java.util.Objects;

/**
 * Value object representing the link between a {@link ReservedBudget} and a source item
 * (Installment or Subscription).
 *
 * <p>A link is effective from {@code fromMonth} onward (inclusive). Before {@code fromMonth},
 * the linked item still deducts directly from the wallet. From {@code fromMonth} onward, the
 * item is absorbed inside the ReservedBudget's ceiling and excluded from direct wallet
 * deduction.</p>
 *
 * <p>Mirrors the applicability semantics of {@link ReservedBudget#isApplicable(YearMonth)}.</p>
 */
public record ReservedBudgetLink(ReservedBudgetLinkSourceType sourceType,
                                  String sourceId,
                                  YearMonth fromMonth) {

    public ReservedBudgetLink {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        if (sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId must not be blank");
        }
        Objects.requireNonNull(fromMonth, "fromMonth must not be null");
    }

    /**
     * Returns {@code true} if this link is effective for the given {@code month}.
     *
     * <p>A link is applicable when {@code month} is not before {@code fromMonth}.</p>
     */
    public boolean isApplicable(YearMonth month) {
        Objects.requireNonNull(month, "month must not be null");
        return !month.isBefore(fromMonth);
    }
}
