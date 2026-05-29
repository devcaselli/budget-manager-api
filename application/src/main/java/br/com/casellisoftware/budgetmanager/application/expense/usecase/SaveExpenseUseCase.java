package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SaveExpenseUseCase implements SaveExpenseBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveExpenseUseCase.class);

    private final ExpenseRepository expenseRepository;
    private final InstallmentExpenseSaver installmentExpenseSaver;
    private final FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;
    private final FindCreditCardByIdBoundary findCreditCardByIdBoundary;

    public SaveExpenseUseCase(ExpenseRepository expenseRepository,
                              InstallmentExpenseSaver installmentExpenseSaver,
                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                              FindCreditCardByIdBoundary findCreditCardByIdBoundary) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository);
        this.installmentExpenseSaver = Objects.requireNonNull(installmentExpenseSaver);
        this.findWalletDomainByIdBoundary = Objects.requireNonNull(findWalletDomainByIdBoundary);
        this.findCreditCardByIdBoundary = Objects.requireNonNull(findCreditCardByIdBoundary);
    }

    public SaveExpenseUseCase(ExpenseRepository expenseRepository,
                              InstallmentRepository installmentRepository,
                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                              FindCreditCardByIdBoundary findCreditCardByIdBoundary) {
        this(
                expenseRepository,
                new InstallmentExpenseSaver(expenseRepository, installmentRepository),
                findWalletDomainByIdBoundary,
                findCreditCardByIdBoundary
        );
    }

    @Override
    public ExpenseOutput execute(ExpenseInput input) {
        Objects.requireNonNull(input, "input must not be null");
        log.info("Saving expense for walletId={} creditCardId={}", input.walletId(), input.creditCardId());

        Wallet wallet = findWalletDomainByIdBoundary.findById(input.walletId(), input.ownerId());
        findCreditCardByIdBoundary.findById(input.creditCardId(), input.ownerId());

        if (Boolean.TRUE.equals(input.installment())) {
            return installmentExpenseSaver.save(wallet, input);
        }

        Expense expense = Expense.create(
                wallet.getId(),
                input.creditCardId(),
                input.name(),
                Money.of(input.cost(), wallet.getBudget().currency()),
                input.purchaseDate(),
                input.flag(),
                false,
                null,
                input.ownerId()
        );

        Expense saved = this.expenseRepository.save(expense);
        log.info("Expense saved successfully, id={}", saved.getId());
        return ExpenseOutputAssembler.from(saved);
    }
}
