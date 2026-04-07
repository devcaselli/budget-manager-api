package br.com.casellisoftware.budgetmanager.domain.expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {

    List<Expense> findAllByWalletId(String walletId);
    Optional<Expense> findById(String id);
    Expense save(Expense expense);
    void delete(Expense expense);
    Expense update(Expense updatedExpense, String expenseId);
}
