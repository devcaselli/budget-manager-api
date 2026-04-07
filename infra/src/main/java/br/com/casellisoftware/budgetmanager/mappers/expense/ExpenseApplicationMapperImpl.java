package br.com.casellisoftware.budgetmanager.mappers.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.mappers.ExpenseApplicationMapper;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpenseApplicationMapperImpl implements ExpenseApplicationMapper {

    private final ExpenseMappers mappers;

    @Override
    public Expense mapToDomain(ExpenseInput input) {
        return this.mappers.expenseInputToExpenseDomain(input);
    }

    @Override
    public ExpenseOutput mapToOutput(Expense domain) {
        return this.mappers.expenseDomainToExpenseOutput(domain);
    }
}
