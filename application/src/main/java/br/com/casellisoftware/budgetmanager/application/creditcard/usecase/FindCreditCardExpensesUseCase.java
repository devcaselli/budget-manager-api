package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardExpensesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardFilter;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseByCreditCardResult;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;

import java.math.BigDecimal;
import java.util.List;

public class FindCreditCardExpensesUseCase implements FindCreditCardExpensesBoundary {

    private final CreditCardRepository creditCardRepository;
    private final ExpenseRepository expenseRepository;
    private final WalletRepository walletRepository;

    public FindCreditCardExpensesUseCase(CreditCardRepository creditCardRepository,
                                         ExpenseRepository expenseRepository,
                                         WalletRepository walletRepository) {
        this.creditCardRepository = creditCardRepository;
        this.expenseRepository = expenseRepository;
        this.walletRepository = walletRepository;
    }

    @Override
    public CreditCardExpensesOutput execute(String creditCardId, FindCreditCardExpensesInput input, String ownerId) {
        creditCardRepository.findById(creditCardId, ownerId)
                .orElseThrow(() -> new CreditCardNotFoundException(creditCardId));

        List<String> walletIds = null;
        if (input.effectiveMonth() != null) {
            walletIds = walletRepository.findIdsByEffectiveMonth(input.effectiveMonth(), ownerId);
            if (walletIds.isEmpty()) {
                return new CreditCardExpensesOutput(
                        new PageResult<>(List.of(), input.page(), input.size(), 0, 0),
                        BigDecimal.ZERO
                );
            }
        }

        ExpenseByCreditCardResult result = expenseRepository.findByCreditCardId(
                creditCardId,
                new ExpenseByCreditCardFilter(walletIds, input.name()),
                input.page(),
                input.size(),
                ownerId
        );

        List<ExpenseOutput> content = result.expenses().content().stream()
                .map(ExpenseOutputAssembler::from)
                .toList();

        return new CreditCardExpensesOutput(
                new PageResult<>(
                        content,
                        result.expenses().page(),
                        result.expenses().size(),
                        result.expenses().totalElements(),
                        result.expenses().totalPages()
                ),
                result.totalCost()
        );
    }
}
