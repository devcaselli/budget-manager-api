package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetVersion;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.math.BigDecimal;

public final class ReservedBudgetOutputAssembler {

    private ReservedBudgetOutputAssembler() {
    }

    /**
     * Assembles an output without consumption figures (consumedAmount/remainingAmount are
     * {@code null}). Used by the paginated list endpoint, which does not compute consumption.
     */
    public static ReservedBudgetOutput from(ReservedBudget reservedBudget) {
        return from(reservedBudget, null, null);
    }

    /**
     * Assembles an output including consumed/remaining figures for a target month.
     */
    public static ReservedBudgetOutput from(ReservedBudget reservedBudget, Money consumed, Money remaining) {
        BigDecimal consumedAmount = consumed == null ? null : consumed.amount();
        BigDecimal remainingAmount = remaining == null ? null : remaining.amount();
        return new ReservedBudgetOutput(
                reservedBudget.getId(),
                reservedBudget.getDescription(),
                reservedBudget.getDetails(),
                reservedBudget.getCurrency().getCurrencyCode(),
                reservedBudget.getStartMonth(),
                reservedBudget.getVersions().stream()
                        .map(ReservedBudgetOutputAssembler::fromVersion)
                        .toList(),
                reservedBudget.getLinks().stream()
                        .map(ReservedBudgetOutputAssembler::fromLink)
                        .toList(),
                reservedBudget.isDeleted(),
                reservedBudget.getFlag(),
                consumedAmount,
                remainingAmount
        );
    }

    private static ReservedBudgetVersionOutput fromVersion(ReservedBudgetVersion version) {
        return new ReservedBudgetVersionOutput(
                version.effectiveMonth(),
                version.amount().amount()
        );
    }

    private static ReservedBudgetLinkOutput fromLink(ReservedBudgetLink link) {
        return new ReservedBudgetLinkOutput(
                link.sourceType(),
                link.sourceId(),
                link.fromMonth()
        );
    }
}
