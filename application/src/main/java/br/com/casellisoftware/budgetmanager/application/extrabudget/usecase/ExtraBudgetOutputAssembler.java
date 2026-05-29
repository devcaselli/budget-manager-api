package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;

import java.util.List;

/**
 * Converts a rich-domain {@link ExtraBudget} into the framework-agnostic
 * {@link ExtraBudgetOutput} consumed by interface adapters.
 *
 * <p>Hand-written on purpose: the flatten {@code Money → BigDecimal} is trivial,
 * and forcing MapStruct here would require {@code expression} attributes that
 * are uglier than the straight code below.</p>
 */
public final class ExtraBudgetOutputAssembler {

    private ExtraBudgetOutputAssembler() {
    }

    public static ExtraBudgetOutput from(ExtraBudget extraBudget) {
        List<AllocationOutput> allocations = extraBudget.getAllocations().stream()
                .map(a -> new AllocationOutput(a.bulletId(), a.amount().amount()))
                .toList();

        return new ExtraBudgetOutput(
                extraBudget.getId(),
                extraBudget.getOwnerId(),
                extraBudget.getDescription(),
                extraBudget.getWalletId(),
                extraBudget.getAmount().amount(),
                extraBudget.getAmount().currency().getCurrencyCode(),
                allocations,
                extraBudget.isDeleted(),
                extraBudget.getDeletedAt()
        );
    }
}
