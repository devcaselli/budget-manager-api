package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.DeleteInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.usecase.DeleteInstallmentUseCase;
import br.com.casellisoftware.budgetmanager.application.installment.usecase.FindInstallmentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.installment.usecase.FindInstallmentsByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.installment.usecase.PatchInstallmentUseCase;
import br.com.casellisoftware.budgetmanager.application.installment.usecase.SaveStandaloneInstallmentUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class InstallmentBeanConfiguration {

    @Bean
    public FindInstallmentByIdBoundary findInstallmentByIdBoundary(InstallmentRepository repo,
                                                                   ShareRepository shareRepository) {
        return new FindInstallmentByIdUseCase(repo, shareRepository);
    }

    @Bean
    public FindInstallmentsByWalletIdBoundary findInstallmentsByWalletIdBoundary(
            InstallmentRepository repo,
            FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
            ShareRepository shareRepository) {
        return new FindInstallmentsByWalletIdUseCase(repo, findWalletDomainByIdBoundary, shareRepository);
    }

    @Bean
    public DeleteInstallmentBoundary deleteInstallmentBoundary(InstallmentRepository repo,
                                                               ExpenseRepository expenseRepository,
                                                               ShareRepository shareRepository,
                                                               FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdBoundary,
                                                               FindAllBulletsByIdsBoundary findAllBulletsByIdsBoundary,
                                                               PatchBulletBoundary patchBulletBoundary,
                                                               DeleteAllPaymentByIdBoundary deleteAllPaymentByIdBoundary,
                                                               Clock clock) {
        DeleteInstallmentUseCase useCase = new DeleteInstallmentUseCase(
                repo,
                expenseRepository,
                shareRepository,
                findAllPaymentByExpenseIdBoundary,
                findAllBulletsByIdsBoundary,
                patchBulletBoundary,
                deleteAllPaymentByIdBoundary,
                clock);
        return new TransactionalDeleteInstallmentBoundary(useCase);
    }

    @Bean
    public SaveStandaloneInstallmentBoundary saveStandaloneInstallmentBoundary(
            InstallmentRepository repo,
            FindCreditCardByIdBoundary findCreditCardByIdBoundary,
            Clock clock) {
        return new SaveStandaloneInstallmentUseCase(repo, findCreditCardByIdBoundary, clock);
    }

    @Bean
    public PatchInstallmentBoundary patchInstallmentBoundary(InstallmentRepository repo,
                                                              FindCreditCardByIdBoundary findCreditCardByIdBoundary) {
        return new PatchInstallmentUseCase(repo, findCreditCardByIdBoundary);
    }
}
