package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpensesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class FindExpensesByWalletIdUseCase implements FindExpensesByWalletIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(FindExpensesByWalletIdUseCase.class);

    private final ExpenseRepository expenseRepository;
    private final FindWalletByIdBoundary findWalletByIdBoundary;

    public FindExpensesByWalletIdUseCase(ExpenseRepository expenseRepository,
                                         FindWalletByIdBoundary findWalletByIdBoundary) {
        this.expenseRepository = expenseRepository;
        this.findWalletByIdBoundary = findWalletByIdBoundary;
    }

    @Override
    public PageResult<ExpenseOutput> execute(String walletId, int page, int size) {
        log.info("Finding expenses for walletId={}, page={}, size={}", walletId, page, size);

        findWalletByIdBoundary.findById(walletId);

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
