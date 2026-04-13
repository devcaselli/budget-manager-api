package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.PatchExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.PatchExpenseInput;
import br.com.casellisoftware.budgetmanager.application.shared.PatchHelper;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PatchExpenseUseCase implements PatchExpenseBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchExpenseUseCase.class);

    private final ExpenseRepository expenseRepository;

    public PatchExpenseUseCase(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Override
    public ExpenseOutput execute(PatchExpenseInput input) {
        log.info("Patching expense id={}", input.id());

        Expense existing = expenseRepository.findById(input.id())
                .orElseThrow(() -> new ExpenseNotFoundException(input.id()));

        Expense patched = PatchHelper.applyPatch(existing, input);

        Expense saved = expenseRepository.save(patched);
        log.info("Expense patched successfully, id={}", saved.getId());

        return ExpenseOutputAssembler.from(saved);
    }
}
