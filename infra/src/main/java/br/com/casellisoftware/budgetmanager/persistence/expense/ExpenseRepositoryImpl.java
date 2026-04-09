package br.com.casellisoftware.budgetmanager.persistence.expense;

import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.persistence.expense.mappers.ExpensePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Dumb adapter: maps entity ↔ document and delegates to Spring Data. No
 * business decisions, no domain exceptions, no input validation beyond what
 * the {@link Expense} entity already guarantees.
 */
@Repository
@RequiredArgsConstructor
public class ExpenseRepositoryImpl implements ExpenseRepository {

    private final ExpenseMongoRepository expenseMongoRepository;
    private final ExpensePersistenceMapper mapper;

    @Override
    public Expense save(Expense expense) {
        ExpenseDocument saved = this.expenseMongoRepository.save(mapper.toDocument(expense));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Expense> findById(String id) {
        return this.expenseMongoRepository.findById(id).map(mapper::toDomain);
    }
}
