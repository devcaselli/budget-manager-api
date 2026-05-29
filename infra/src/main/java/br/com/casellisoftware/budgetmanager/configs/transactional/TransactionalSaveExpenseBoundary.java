package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import org.springframework.transaction.annotation.Transactional;

public class TransactionalSaveExpenseBoundary implements SaveExpenseBoundary {

    private final SaveExpenseBoundary delegate;

    public TransactionalSaveExpenseBoundary(SaveExpenseBoundary delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public ExpenseOutput execute(ExpenseInput input) {
        return delegate.execute(input);
    }
}
