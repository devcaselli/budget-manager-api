package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindAllBulletsByIdsUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindBulletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.PatchBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.SaveBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.DeleteExpenseByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpenseByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpensesByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.PatchExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeleteAllPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeletePaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindAllPaymentByExpenseIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.PayExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
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
    public DeleteExpenseByIdUseCase deleteExpenseByIdUseCase(
            ExpenseRepository expenseRepository,
            FindExpenseByIdUseCase findExpenseByIdUseCase,
            FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary,
            FindAllBulletsByIdsUseCase findAllBulletsByIdsUseCase,
            PatchBulletUseCase patchBulletUseCase,
            DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase) {
        return new DeleteExpenseByIdUseCase(
                expenseRepository,
                findExpenseByIdUseCase,
                findAllPaymentByExpenseIdBoundary,
                findAllBulletsByIdsUseCase,
                patchBulletUseCase,
                deleteAllPaymentByIdUseCase);
    }

    @Bean
    public SaveBulletUseCase saveBulletUseCase(BulletRepository repository, FindWalletByIdBoundary findWalletByIdBoundary) {
        return new SaveBulletUseCase(repository, findWalletByIdBoundary);
    }

    @Bean
    public FindBulletByIdUseCase findBulletByIdUseCase(BulletRepository repository) {
        return new FindBulletByIdUseCase(repository);
    }

    @Bean
    public PatchBulletUseCase patchBulletUseCase(BulletRepository repository) {
        return new PatchBulletUseCase(repository);
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
    public FindWalletByIdBoundary findWalletByIdBoundary(WalletRepository walletRepository) {
        return new FindWalletByIdUseCase(walletRepository);
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
    public DeletePaymentByIdUseCase deletePaymentByIdUseCase(PaymentRepository paymentRepository, FindPaymentByIdUseCase findPaymentByIdUseCase) {
        return new DeletePaymentByIdUseCase(paymentRepository, findPaymentByIdUseCase);
    }

    @Bean
    public DeleteAllPaymentByIdUseCase deleteAllPaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new DeleteAllPaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public PayExpenseUseCase payExpenseUseCase(PaymentRepository paymentRepository,
                                               ExpenseRepository expenseRepository,
                                               BulletRepository bulletRepository) {
        return new PayExpenseUseCase(paymentRepository, expenseRepository, bulletRepository);
    }
}
