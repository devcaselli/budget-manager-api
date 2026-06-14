package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetVersion;

public final class ReservedBudgetOutputAssembler {

    private ReservedBudgetOutputAssembler() {
    }

    public static ReservedBudgetOutput from(ReservedBudget reservedBudget) {
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
                reservedBudget.getFlag()
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
