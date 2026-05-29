package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindActiveSubscriptionsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindAllSubscriptionsBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindSubscriptionByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindSubscriptionDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.DeleteSubscriptionUseCase;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.FindActiveSubscriptionsByMonthUseCase;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.FindAllSubscriptionsUseCase;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.FindSubscriptionByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.FindSubscriptionDomainByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.PatchSubscriptionUseCase;
import br.com.casellisoftware.budgetmanager.application.subscription.usecase.SaveSubscriptionUseCase;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.FindSubscriptionChargesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.usecase.FindSubscriptionChargesByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SubscriptionBeanConfiguration {

    @Bean
    public SaveSubscriptionBoundary saveSubscriptionBoundary(SubscriptionRepository subscriptionRepository,
                                                             CreditCardRepository creditCardRepository,
                                                             Clock clock) {
        SaveSubscriptionUseCase useCase = new SaveSubscriptionUseCase(subscriptionRepository, creditCardRepository, clock);
        return new TransactionalSaveSubscriptionBoundary(useCase);
    }

    @Bean
    public PatchSubscriptionBoundary patchSubscriptionBoundary(SubscriptionRepository subscriptionRepository,
                                                               Clock clock) {
        PatchSubscriptionUseCase useCase = new PatchSubscriptionUseCase(subscriptionRepository, clock);
        return new TransactionalPatchSubscriptionBoundary(useCase);
    }

    @Bean
    public DeleteSubscriptionBoundary deleteSubscriptionBoundary(SubscriptionRepository subscriptionRepository,
                                                                 br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository shareRepository,
                                                                 Clock clock,
                                                                 FlagManager flagManager) {
        DeleteSubscriptionUseCase useCase = new DeleteSubscriptionUseCase(subscriptionRepository, shareRepository, clock, flagManager);
        return new TransactionalDeleteSubscriptionBoundary(useCase);
    }

    @Bean
    public FindSubscriptionByIdBoundary findSubscriptionByIdBoundary(SubscriptionRepository subscriptionRepository) {
        return new FindSubscriptionByIdUseCase(subscriptionRepository);
    }

    @Bean
    public FindSubscriptionDomainByIdBoundary findSubscriptionDomainByIdBoundary(SubscriptionRepository subscriptionRepository) {
        return new FindSubscriptionDomainByIdUseCase(subscriptionRepository);
    }

    @Bean
    public FindAllSubscriptionsBoundary findAllSubscriptionsBoundary(SubscriptionRepository subscriptionRepository) {
        return new FindAllSubscriptionsUseCase(subscriptionRepository);
    }

    @Bean
    public FindActiveSubscriptionsByMonthBoundary findActiveSubscriptionsByMonthBoundary(SubscriptionRepository subscriptionRepository) {
        return new FindActiveSubscriptionsByMonthUseCase(subscriptionRepository);
    }

    @Bean
    public FindSubscriptionChargesByWalletIdBoundary findSubscriptionChargesByWalletIdBoundary(
            SubscriptionRepository subscriptionRepository,
            FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
            ShareRepository shareRepository) {
        return new FindSubscriptionChargesByWalletIdUseCase(subscriptionRepository, findWalletDomainByIdBoundary, shareRepository);
    }
}
