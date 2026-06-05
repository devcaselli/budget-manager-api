package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.payer.usecase.EnsureTransientPayerUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindActiveShareBySourceBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindAllSharesByOwnerBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindShareByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindWalletSharesBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.RevertShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.SaveShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.StopWalletShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.FindActiveShareBySourceUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.FindAllSharesByOwnerUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.FindShareByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.FindWalletSharesUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.RevertShareUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.SaveShareUseCase;
import br.com.casellisoftware.budgetmanager.application.sharing.usecase.StopWalletShareUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalRevertShareBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveShareBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalStopWalletShareBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SharingBeanConfiguration {

    @Bean
    public SaveShareBoundary saveShareBoundary(ShareRepository shareRepository,
                                               ExpenseRepository expenseRepository,
                                               SubscriptionRepository subscriptionRepository,
                                               InstallmentRepository installmentRepository,
                                               PaymentRepository paymentRepository,
                                               PayerRepository payerRepository,
                                               EnsureTransientPayerUseCase ensureTransientPayerUseCase,
                                               Clock clock) {
        SaveShareUseCase useCase = new SaveShareUseCase(
                shareRepository,
                expenseRepository,
                subscriptionRepository,
                installmentRepository,
                paymentRepository,
                payerRepository,
                ensureTransientPayerUseCase,
                clock
        );
        return new TransactionalSaveShareBoundary(useCase);
    }

    @Bean
    public RevertShareBoundary revertShareBoundary(ShareRepository shareRepository,
                                                   PaymentRepository paymentRepository,
                                                   ExpenseRepository expenseRepository,
                                                   br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository installmentRepository,
                                                   Clock clock) {
        return new TransactionalRevertShareBoundary(
                new RevertShareUseCase(shareRepository, paymentRepository, expenseRepository,
                        installmentRepository, clock)
        );
    }

    @Bean
    public FindShareByIdBoundary findShareByIdBoundary(ShareRepository shareRepository,
                                                       PayerRepository payerRepository) {
        return new FindShareByIdUseCase(shareRepository, payerRepository);
    }

    @Bean
    public FindActiveShareBySourceBoundary findActiveShareBySourceBoundary(ShareRepository shareRepository,
                                                                           PayerRepository payerRepository) {
        return new FindActiveShareBySourceUseCase(shareRepository, payerRepository);
    }

    @Bean
    public FindAllSharesByOwnerBoundary findAllSharesByOwnerBoundary(ShareRepository shareRepository,
                                                                     PayerRepository payerRepository) {
        return new FindAllSharesByOwnerUseCase(shareRepository, payerRepository);
    }

    @Bean
    public FindWalletSharesBoundary findWalletSharesBoundary(FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                             SubscriptionRepository subscriptionRepository,
                                                             InstallmentRepository installmentRepository,
                                                             ShareRepository shareRepository,
                                                             PayerRepository payerRepository) {
        return new FindWalletSharesUseCase(findWalletDomainByIdBoundary, subscriptionRepository,
                installmentRepository, shareRepository, payerRepository);
    }

    @Bean
    public StopWalletShareBoundary stopWalletShareBoundary(ShareRepository shareRepository,
                                                           FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        return new TransactionalStopWalletShareBoundary(
                new StopWalletShareUseCase(shareRepository, findWalletDomainByIdBoundary)
        );
    }
}
