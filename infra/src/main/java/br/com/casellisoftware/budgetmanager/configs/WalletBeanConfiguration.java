package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.wallet.RepositoryBackedWalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.WalletDeductionsQuery;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.PatchWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindAllWalletsUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletDomainByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.PatchWalletUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.SaveWalletUseCase;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class WalletBeanConfiguration {

    @Bean
    public WalletDeductionsQuery walletDeductionsQuery(SubscriptionRepository subscriptionRepository,
                                                       InstallmentRepository installmentRepository,
                                                       ShareRepository shareRepository,
                                                       ReservedBudgetRepository reservedBudgetRepository) {
        return new RepositoryBackedWalletDeductionsQuery(
                subscriptionRepository,
                installmentRepository,
                shareRepository,
                reservedBudgetRepository);
    }

    @Bean
    public SaveWalletBoundary saveWalletBoundary(WalletRepository walletRepository,
                                                 WalletDeductionsQuery walletDeductionsQuery,
                                                 Clock clock) {
        SaveWalletUseCase useCase = new SaveWalletUseCase(
                walletRepository,
                walletDeductionsQuery,
                clock
        );
        return new TransactionalSaveWalletBoundary(useCase);
    }

    @Bean
    public PatchWalletBoundary patchWalletBoundary(WalletRepository walletRepository,
                                                   WalletDeductionsQuery walletDeductionsQuery,
                                                   Clock clock) {
        return new PatchWalletUseCase(walletRepository, walletDeductionsQuery, clock);
    }

    @Bean
    public FindAllWalletsBoundary findAllWalletsBoundary(WalletRepository walletRepository,
                                                         WalletDeductionsQuery walletDeductionsQuery) {
        return new FindAllWalletsUseCase(walletRepository, walletDeductionsQuery);
    }

    @Bean
    public FindWalletByIdBoundary findWalletByIdBoundary(WalletRepository walletRepository,
                                                         WalletDeductionsQuery walletDeductionsQuery) {
        return new FindWalletByIdUseCase(walletRepository, walletDeductionsQuery);
    }

    @Bean
    public FindWalletDomainByIdBoundary findWalletDomainByIdBoundary(WalletRepository walletRepository) {
        return new FindWalletDomainByIdUseCase(walletRepository);
    }
}
