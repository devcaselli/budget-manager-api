package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.mappers.ExpenseApplicationMapper;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static br.com.casellisoftware.budgetmanager.application.configs.ApplicationConstants.SAVE_EXPENSE;


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
        log.info(SAVE_EXPENSE, input);
        Expense domain = this.mapper.mapToDomain(input);
        domain = this.expenseRepository.save(domain);
        return this.mapper.mapToOutput(domain);
    }
}
