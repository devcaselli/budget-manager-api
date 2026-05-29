package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindAllMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class FindAllMineExpensesUseCase implements FindAllMineExpensesBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindAllMineExpensesUseCase.class);

    private final ExpenseRepository expenseRepository;
    private final ExpenseOutputEnricher outputEnricher;

    public FindAllMineExpensesUseCase(ExpenseRepository expenseRepository,
                                      InstallmentRepository installmentRepository) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.outputEnricher = new ExpenseOutputEnricher(installmentRepository);
    }

    @Override
    public List<ExpenseOutput> execute(String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        log.debug("Finding all mine expenses ownerId={}", ownerId);
        List<Expense> expenses = expenseRepository.findAllByOwnerId(ownerId);
        return outputEnricher.toOutputs(expenses, ownerId);
    }
}
