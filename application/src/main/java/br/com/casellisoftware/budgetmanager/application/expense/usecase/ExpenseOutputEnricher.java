package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class ExpenseOutputEnricher {

    private final InstallmentRepository installmentRepository;

    ExpenseOutputEnricher(InstallmentRepository installmentRepository) {
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
    }

    List<ExpenseOutput> toOutputs(List<Expense> expenses, String ownerId) {
        Map<String, Integer> installmentNumbers = installmentNumbers(expenses, ownerId);
        return expenses.stream()
                .map(expense -> ExpenseOutputAssembler.from(
                        expense,
                        installmentNumbers.get(expense.getInstallmentId())))
                .toList();
    }

    private Map<String, Integer> installmentNumbers(List<Expense> expenses, String ownerId) {
        Map<String, Integer> installmentNumbers = new HashMap<>();
        expenses.stream()
                .map(Expense::getInstallmentId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(installmentId -> installmentRepository.findById(installmentId, ownerId)
                        .map(Installment::getInstallmentNumber)
                        .ifPresent(installmentNumber -> installmentNumbers.put(installmentId, installmentNumber)));
        return installmentNumbers;
    }
}
