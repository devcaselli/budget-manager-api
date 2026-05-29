package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;

public class FindMineExpensesUseCase implements FindMineExpensesBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindMineExpensesUseCase.class);

    private final ExpenseRepository expenseRepository;
    private final Clock clock;
    private final ExpenseOutputEnricher outputEnricher;

    public FindMineExpensesUseCase(ExpenseRepository expenseRepository,
                                   InstallmentRepository installmentRepository,
                                   Clock clock) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.outputEnricher = new ExpenseOutputEnricher(installmentRepository);
    }

    @Override
    public List<ExpenseOutput> execute(int months, String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        YearMonth oldestMonth = YearMonth.now(clock).minusMonths(months - 1L);
        LocalDate startDate = oldestMonth.atDay(1);

        log.debug("Finding mine expenses ownerId={} months={} startDate={}", ownerId, months, startDate);
        List<Expense> expenses = expenseRepository.findByOwnerIdAndPurchaseDateGreaterThanOrEqual(ownerId, startDate);
        return outputEnricher.toOutputs(expenses, ownerId);
    }
}
