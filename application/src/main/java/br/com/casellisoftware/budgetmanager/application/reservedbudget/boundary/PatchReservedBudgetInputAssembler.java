package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetPatch;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Currency;

/**
 * Bridges the application patch contract to the domain-native {@link ReservedBudgetPatch},
 * keeping the aggregate free from application DTO dependencies.
 */
public final class PatchReservedBudgetInputAssembler {

    private PatchReservedBudgetInputAssembler() {
    }

    public static ReservedBudgetPatch toPatch(PatchReservedBudgetInput input, Currency currency) {
        return ReservedBudgetPatch.empty()
                .withDescription(input.description())
                .withDetails(input.details())
                .withNewAmount(input.newAmount() == null ? null : Money.of(input.newAmount(), currency))
                .withFlag(input.flag());
    }
}
