package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpensesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletByIdUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FindExpensesByWalletIdUseCase implements FindExpensesByWalletIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindExpensesByWalletIdUseCase.class);

    private final ExpenseRepository expenseRepository;
    private final FindWalletByIdUseCase findWalletByIdUseCase;

    public FindExpensesByWalletIdUseCase(ExpenseRepository expenseRepository,
                                         FindWalletByIdUseCase findWalletByIdUseCase) {
        this.expenseRepository = expenseRepository;
        this.findWalletByIdUseCase = findWalletByIdUseCase;
    }

    @Override
    public PageResult<ExpenseOutput> execute(String walletId, int page, int size) {
        log.info("Finding expenses for walletId={}, page={}, size={}", walletId, page, size);

        findWalletByIdUseCase.execute(walletId);

        PageResult<Expense> expensePage = expenseRepository.findByWalletId(walletId, page, size);

        List<ExpenseOutput> outputs = expensePage.content().stream()
                .map(ExpenseOutputAssembler::from)
                .toList();

        log.info("Found {} expenses for walletId={} (page {}/{})",
                outputs.size(), walletId, page, expensePage.totalPages());

        return new PageResult<>(
                outputs,
                expensePage.page(),
                expensePage.size(),
                expensePage.totalElements(),
                expensePage.totalPages()
        );
    }
}
