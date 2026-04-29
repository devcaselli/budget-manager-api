package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.DeleteBulletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindAllBulletsByIdsUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindBulletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.PatchBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.SaveBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.DeleteExpenseByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpenseByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpensesByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.PatchExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeleteAllPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeletePaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindAllPaymentByExpenseIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.PayExpenseUseCase;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteExpenseBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindAllWalletsUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletDomainByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.SaveWalletUseCase;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessLayerBeanConfiguration {

    @Bean
    public SaveExpenseUseCase saveExpenseUseCase(ExpenseRepository repository, FindWalletByIdBoundary findWalletByIdBoundary) {
        return new SaveExpenseUseCase(repository, findWalletByIdBoundary);
    }

    @Bean
    public FindExpensesByWalletIdUseCase findExpensesByWalletIdUseCase(ExpenseRepository repository, FindWalletByIdBoundary findWalletByIdBoundary) {
        return new FindExpensesByWalletIdUseCase(repository, findWalletByIdBoundary);
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
            DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary) {
        DeleteExpenseByIdUseCase useCase = new DeleteExpenseByIdUseCase(
                expenseRepository,
                findAllPaymentByExpenseIdBoundary,
                findAllBulletsByIdsBoundary,
                patchBulletBoundary,
                deleteAllPaymentByIdBoundary);
        return new TransactionalDeleteExpenseBoundary(useCase);
    }

    @Bean
    public SaveBulletBoundary saveBulletBoundary(BulletRepository bulletRepository,
                                                 WalletRepository walletRepository,
                                                 FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        SaveBulletUseCase useCase = new SaveBulletUseCase(
                bulletRepository,
                walletRepository,
                findWalletDomainByIdBoundary);
        return new TransactionalSaveBulletBoundary(useCase);
    }

    @Bean
    public FindBulletByIdUseCase findBulletByIdUseCase(BulletRepository repository) {
        return new FindBulletByIdUseCase(repository);
    }

    @Bean
    public PatchBulletBoundary patchBulletBoundary(BulletRepository bulletRepository,
                                                   WalletRepository walletRepository,
                                                   FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        PatchBulletUseCase useCase = new PatchBulletUseCase(
                bulletRepository,
                walletRepository,
                findWalletDomainByIdBoundary);
        return new TransactionalPatchBulletBoundary(useCase);
    }

    @Bean
    public DeleteBulletByIdBoundary deleteBulletByIdBoundary(BulletRepository bulletRepository,
                                                             WalletRepository walletRepository,
                                                             PaymentRepository paymentRepository,
                                                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        DeleteBulletByIdUseCase useCase = new DeleteBulletByIdUseCase(
                bulletRepository,
                walletRepository,
                paymentRepository,
                findWalletDomainByIdBoundary);
        return new TransactionalDeleteBulletByIdBoundary(useCase);
    }

    @Bean
    public FindAllBulletsByIdsUseCase findAllBulletsByIdsUseCase(BulletRepository repository) {
        return new FindAllBulletsByIdsUseCase(repository);
    }

    @Bean
    public SaveWalletUseCase saveWalletBoundary(WalletRepository walletRepository) {
        return new SaveWalletUseCase(walletRepository);
    }

    @Bean
    public FindAllWalletsBoundary findAllWalletsBoundary(WalletRepository walletRepository) {
        return new FindAllWalletsUseCase(walletRepository);
    }

    @Bean
    public FindWalletByIdBoundary findWalletByIdBoundary(WalletRepository walletRepository) {
        return new FindWalletByIdUseCase(walletRepository);
    }

    @Bean
    public FindWalletDomainByIdBoundary findWalletDomainByIdBoundary(WalletRepository walletRepository) {
        return new FindWalletDomainByIdUseCase(walletRepository);
    }

    @Bean
    public FindPaymentByIdUseCase findPaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new FindPaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public FindAllPaymentByExpenseIdUseCase findAllPaymentByExpenseIdUseCase(PaymentRepository paymentRepository) {
        return new FindAllPaymentByExpenseIdUseCase(paymentRepository);
    }

    @Bean
    public DeletePaymentByIdUseCase deletePaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new DeletePaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new DeleteAllPaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public PayExpenseBoundary payExpenseBoundary(PaymentRepository paymentRepository,
                                                 ExpenseRepository expenseRepository,
                                                 BulletRepository bulletRepository) {
        PayExpenseUseCase useCase = new PayExpenseUseCase(paymentRepository, expenseRepository, bulletRepository);
        return new TransactionalPayExpenseBoundary(useCase);
    }
}
