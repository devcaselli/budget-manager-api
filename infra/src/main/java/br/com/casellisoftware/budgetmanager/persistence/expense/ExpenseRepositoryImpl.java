package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final ExpenseMongoRepository expenseMongoRepository;
    private final ExpensePersistenceMapper mapper;

    @Override
    public List<Expense> findAllByWalletId(String walletId) {
        return this.expenseMongoRepository.findAllByWalletId(walletId)
                .stream().map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Expense> findById(String id) {
        return this.expenseMongoRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Expense save(Expense expense) {
        ExpenseDocument saved = this.expenseMongoRepository.save(mapper.toDocument(expense));
        return mapper.toDomain(saved);
    }

    @Override
    public void delete(Expense expense) {
        if (expense == null || expense.getId() == null) {
            throw new IllegalArgumentException("Cannot delete expense without id");
        }
        if (!this.expenseMongoRepository.existsById(expense.getId())) {
            throw new ExpenseNotFoundException(expense.getId());
        }
        this.expenseMongoRepository.deleteById(expense.getId());
    }

    @Override
    public Expense update(Expense updatedExpense, String expenseId) {
        if (expenseId == null) {
            throw new IllegalArgumentException("expenseId must not be null");
        }
        if (!this.expenseMongoRepository.existsById(expenseId)) {
            throw new ExpenseNotFoundException(expenseId);
        }
        ExpenseDocument document = mapper.toDocument(updatedExpense);
        document.setId(expenseId);
        ExpenseDocument saved = this.expenseMongoRepository.save(document);
        return mapper.toDomain(saved);
    }
}
