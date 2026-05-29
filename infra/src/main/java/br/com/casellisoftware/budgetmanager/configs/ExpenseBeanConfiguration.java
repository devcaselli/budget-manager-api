package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindAllMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.DeleteExpenseByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpenseByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindAllMineExpensesUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpensesByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindMineExpensesUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.InstallmentExpenseSaver;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.PatchExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteExpenseBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ExpenseBeanConfiguration {

    @Bean
    public SaveExpenseBoundary saveExpenseBoundary(ExpenseRepository repository,
                                                   InstallmentRepository installmentRepository,
                                                   FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                   FindCreditCardByIdBoundary findCreditCardByIdBoundary) {
        SaveExpenseUseCase useCase = new SaveExpenseUseCase(
                repository,
                new InstallmentExpenseSaver(repository, installmentRepository),
                findWalletDomainByIdBoundary,
                findCreditCardByIdBoundary
        );
        return new TransactionalSaveExpenseBoundary(useCase);
    }

    @Bean
    public FindExpensesByWalletIdUseCase findExpensesByWalletIdUseCase(ExpenseRepository repository,
                                                                       FindWalletByIdBoundary findWalletByIdBoundary) {
        return new FindExpensesByWalletIdUseCase(repository, findWalletByIdBoundary);
    }

    @Bean
    public FindMineExpensesBoundary findMineExpensesBoundary(ExpenseRepository repository,
                                                             InstallmentRepository installmentRepository,
                                                             Clock clock) {
        return new FindMineExpensesUseCase(repository, installmentRepository, clock);
    }

    @Bean
    public FindAllMineExpensesBoundary findAllMineExpensesBoundary(ExpenseRepository repository,
                                                                   InstallmentRepository installmentRepository) {
        return new FindAllMineExpensesUseCase(repository, installmentRepository);
    }

    @Bean
    public FindExpenseByIdUseCase findExpenseByIdUseCase(ExpenseRepository repository) {
        return new FindExpenseByIdUseCase(repository);
    }

    @Bean
    public PatchExpenseUseCase patchExpenseUseCase(ExpenseRepository repository) {
        return new PatchExpenseUseCase(repository);
    }

    @Bean
    public DeleteExpenseByIdBoundary deleteExpenseByIdBoundary(
            ExpenseRepository expenseRepository,
            FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary,
            FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary,
            PatchBulletBoundary patchBulletBoundary,
            DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary,
            br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository shareRepository) {
        DeleteExpenseByIdUseCase useCase = new DeleteExpenseByIdUseCase(
                expenseRepository,
                findAllPaymentByExpenseIdBoundary,
                findAllBulletsByIdsBoundary,
                patchBulletBoundary,
                deleteAllPaymentByIdBoundary,
                shareRepository);
        return new TransactionalDeleteExpenseBoundary(useCase);
    }
}
