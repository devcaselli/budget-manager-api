package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletByIdUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SaveExpenseUseCase implements SaveExpenseBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveExpenseUseCase.class);

    private final ExpenseRepository expenseRepository;
    private final FindWalletByIdUseCase  findWalletByIdUseCase;

    public SaveExpenseUseCase(ExpenseRepository expenseRepository, FindWalletByIdUseCase findWalletByIdUseCase) {
        this.expenseRepository = expenseRepository;
        this.findWalletByIdUseCase = findWalletByIdUseCase;
    }

    @Override
    public ExpenseOutput execute(ExpenseInput input) {
        log.info("Saving expense for walletId={}", input.walletId());

        Expense expense = Expense.create(
                checkIfWalletExists(input.walletId()),
                input.name(),
                Money.of(input.cost()),
                input.purchaseDate()
        );

        Expense saved = this.expenseRepository.save(expense);
        log.info("Expense saved successfully, id={}", saved.getId());

        return ExpenseOutputAssembler.from(saved);
    }

    private String checkIfWalletExists(String walletId){
        WalletOutput execute = this.findWalletByIdUseCase.execute(walletId);
        return execute.id();
    }
}
