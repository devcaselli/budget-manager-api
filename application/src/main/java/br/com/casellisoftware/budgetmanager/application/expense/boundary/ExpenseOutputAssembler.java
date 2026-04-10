package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;

/**
 * Converts a rich-domain {@link Expense} into the framework-agnostic
 * {@link ExpenseOutput} consumed by interface adapters.
 *
 * <p>Lives in the application layer (not in a use case, not in the domain)
 * because:
 * <ul>
 *   <li>Use cases must not own mapping — that's an SRP violation and guarantees
 *       duplication across {@code SaveExpenseUseCase}, {@code GetExpenseUseCase},
 *       {@code ListExpensesUseCase}, etc.</li>
 *   <li>The domain must not know about boundary DTOs — that would invert the
 *       dependency direction.</li>
 * </ul>
 *
 * <p>Hand-written on purpose: the flatten {@code Money → BigDecimal} is trivial,
 * and forcing MapStruct here would require {@code expression} attributes that
 * are uglier than the straight code below.</p>
 */
public final class ExpenseOutputAssembler {

    private ExpenseOutputAssembler() {
    }

    public static ExpenseOutput from(Expense expense) {
        return new ExpenseOutput(
                expense.getId(),
                expense.getName(),
                expense.getCost().amount(),
                expense.getPurchaseDate(),
                expense.getWalletId(),
                expense.getRemaining().amount()
        );
    }
}
