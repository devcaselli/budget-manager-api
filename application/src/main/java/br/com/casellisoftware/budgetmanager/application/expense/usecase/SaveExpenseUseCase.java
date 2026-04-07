package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.mappers.ExpenseApplicationMapper;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static br.com.casellisoftware.budgetmanager.application.configs.ApplicationConstants.SAVE_EXPENSE_START;
import static br.com.casellisoftware.budgetmanager.application.configs.ApplicationConstants.SAVE_EXPENSE_SUCCESS;


public class SaveExpenseUseCase implements SaveExpenseBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveExpenseUseCase.class);

    private final ExpenseApplicationMapper mapper;
    private final ExpenseRepository expenseRepository;


    public SaveExpenseUseCase(ExpenseApplicationMapper mapper, ExpenseRepository expenseRepository) {
        this.mapper = mapper;
        this.expenseRepository = expenseRepository;
    }

    @Override
    public ExpenseOutput execute(ExpenseInput input) {
        log.info(SAVE_EXPENSE_START, input.walletId());
        Expense domain = this.mapper.mapToDomain(input);
        domain = this.expenseRepository.save(domain);
        log.info(SAVE_EXPENSE_SUCCESS, domain.getId());
        return this.mapper.mapToOutput(domain);
    }
}
