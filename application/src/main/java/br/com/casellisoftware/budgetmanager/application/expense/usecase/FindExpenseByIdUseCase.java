package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;

public class FindExpenseByIdUseCase implements FindExpenseByIdBoundary {

    private final ExpenseRepository expenseRepository;

    public FindExpenseByIdUseCase(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }


    public ExpenseOutput execute(String id){
        Expense expense = this.expenseRepository.findById(id)
                .orElseThrow(() -> new ExpenseNotFoundException(id));

        return ExpenseOutputAssembler.from(expense);
    }
}
