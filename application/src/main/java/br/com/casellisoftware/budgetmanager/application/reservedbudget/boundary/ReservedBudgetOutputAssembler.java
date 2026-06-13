package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
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
}
