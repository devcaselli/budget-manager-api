package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.mappers.expense.ExpenseMappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final ExpenseMongoRepository expenseMongoRepository;
    private final ExpenseMappers mapper;


    @Override
    public List<Expense> findAllByWalletId(String walletId) {
        return this.expenseMongoRepository.findAllByWalletId(walletId)
                .stream().map(mapper::expenseDocumentToExpense)
                .toList();
    }

    @Override
    public Optional<Expense> findById(String id) {
        return this.expenseMongoRepository.findById(id)
                .map(mapper::expenseDocumentToExpense);
    }

    @Override
    public Expense save(Expense expense) {
        ExpenseDocument saved = this.expenseMongoRepository.save(mapper.expenseDomainToExpenseDocument(expense));
        return mapper.expenseDocumentToExpense(saved);
    }

    @Override
    public void delete(Expense expense) {
        this.expenseMongoRepository.deleteById(expense.getId());
    }

    @Override
    public Expense update(Expense updatedExpense, String expenseId) {
        updatedExpense.setId(expenseId);
        ExpenseDocument saved = this.expenseMongoRepository.save(mapper.expenseDomainToExpenseDocument(updatedExpense));
        return mapper.expenseDocumentToExpense(saved);
    }
}
