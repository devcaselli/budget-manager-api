package br.com.casellisoftware.budgetmanager.application.expense.boundary;

import br.com.casellisoftware.budgetmanager.domain.expense.ExpensePatch;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

public final class PatchExpenseInputAssembler {

    private PatchExpenseInputAssembler() {
    }

    public static ExpensePatch toPatch(PatchExpenseInput input) {
        return ExpensePatch.empty()
                .withName(input.name())
                .withCost(input.cost() == null ? null : Money.of(input.cost()))
                .withPurchaseDate(input.purchaseDate());
    }
}
