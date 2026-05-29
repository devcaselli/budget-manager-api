package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentFactory;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNumberPolicy;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class InstallmentExpenseSaver {

    private static final Logger log = LoggerFactory.getLogger(InstallmentExpenseSaver.class);

    private final ExpenseRepository expenseRepository;
    private final InstallmentRepository installmentRepository;

    public InstallmentExpenseSaver(ExpenseRepository expenseRepository,
                                   InstallmentRepository installmentRepository) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "expenseRepository must not be null");
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
    }

    public ExpenseOutput save(Wallet wallet, ExpenseInput input) {
        Objects.requireNonNull(wallet, "wallet must not be null");
        Objects.requireNonNull(input, "input must not be null");

        Integer installmentNumber = Objects.requireNonNull(
                input.installmentNumber(),
                "installmentNumber must not be null when installment is true"
        );
        InstallmentNumberPolicy.validate(installmentNumber);

        Expense sourceExpense = expenseRepository.save(Expense.create(
                wallet.getId(),
                input.creditCardId(),
                input.name(),
                Money.of(input.cost(), wallet.getBudget().currency()),
                input.purchaseDate(),
                input.flag(),
                true,
                null,
                input.ownerId()
        ));
        Installment installment = InstallmentFactory.fromExpense(
                sourceExpense,
                installmentNumber,
                wallet.getEffectiveMonth(),
                input.flag()
        );
        Installment savedInstallment = installmentRepository.save(installment);
        Expense installmentExpense = Expense.create(
                wallet.getId(),
                input.creditCardId(),
                input.name(),
                savedInstallment.getInstallmentValue(),
                input.purchaseDate(),
                input.flag(),
                false,
                savedInstallment.getId(),
                input.ownerId()
        );
        Expense savedExpense = expenseRepository.save(installmentExpense);

        log.info("Installment expense saved expenseId={} installmentId={} sourceExpenseId={} number={}",
                savedExpense.getId(), savedInstallment.getId(),
                sourceExpense.getId(), savedInstallment.getInstallmentNumber());
        return ExpenseOutputAssembler.from(savedExpense, savedInstallment.getInstallmentNumber());
    }
}
