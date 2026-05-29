package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindAllBulletsByIdsBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.DefaultPatchBulletStrategy;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.DefaultSaveBulletStrategy;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.DeleteBulletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindAllBulletsByIdsUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindBulletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.FindBulletsByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.PatchBulletIgnoreSubscriptionReservationStrategy;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.PatchBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.SaveBulletIgnoreSubscriptionReservationStrategy;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.SaveBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.flag.FlagAwareExecutor;
import br.com.casellisoftware.budgetmanager.application.flag.FlagStrategyRegistry;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BulletBeanConfiguration {

    // -----------------------------------------------------------------------
    // Save-bullet strategy chain
    // -----------------------------------------------------------------------

    @Bean
    public DefaultSaveBulletStrategy defaultSaveBulletStrategy(BulletRepository bulletRepository,
                                                               WalletRepository walletRepository,
                                                               FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                               SubscriptionRepository subscriptionRepository,
                                                               ShareRepository shareRepository) {
        return new DefaultSaveBulletStrategy(bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository, shareRepository);
    }

    @Bean
    public SaveBulletIgnoreSubscriptionReservationStrategy saveBulletIgnoreSubscriptionReservationStrategy(BulletRepository bulletRepository,
                                                                                                            WalletRepository walletRepository,
                                                                                                            FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                                                                            SubscriptionRepository subscriptionRepository) {
        return new SaveBulletIgnoreSubscriptionReservationStrategy(bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository);
    }

    @Bean
    public FlagStrategyRegistry<BulletInput, BulletOutput> saveBulletStrategyRegistry(
            DefaultSaveBulletStrategy defaultSaveBulletStrategy,
            SaveBulletIgnoreSubscriptionReservationStrategy saveBulletIgnoreSubscriptionReservationStrategy) {
        return FlagStrategyRegistry.<BulletInput, BulletOutput>builder()
                .withDefault(defaultSaveBulletStrategy)
                .register(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, saveBulletIgnoreSubscriptionReservationStrategy)
                .build();
    }

    @Bean
    public FlagAwareExecutor<BulletInput, BulletOutput> saveBulletExecutor(FlagManager flagManager,
                                                                           FlagStrategyRegistry<BulletInput, BulletOutput> saveBulletStrategyRegistry) {
        return new FlagAwareExecutor<>(flagManager, saveBulletStrategyRegistry);
    }

    @Bean
    public SaveBulletBoundary saveBulletBoundary(FlagAwareExecutor<BulletInput, BulletOutput> saveBulletExecutor,
                                                 FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        SaveBulletUseCase useCase = new SaveBulletUseCase(saveBulletExecutor, findWalletDomainByIdBoundary);
        return new TransactionalSaveBulletBoundary(useCase);
    }

    // -----------------------------------------------------------------------
    // Patch-bullet strategy chain
    // -----------------------------------------------------------------------

    @Bean
    public DefaultPatchBulletStrategy defaultPatchBulletStrategy(BulletRepository bulletRepository,
                                                                  WalletRepository walletRepository,
                                                                  FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                                  SubscriptionRepository subscriptionRepository,
                                                                  ShareRepository shareRepository) {
        return new DefaultPatchBulletStrategy(bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository, shareRepository);
    }

    @Bean
    public PatchBulletIgnoreSubscriptionReservationStrategy patchBulletIgnoreSubscriptionReservationStrategy(BulletRepository bulletRepository,
                                                                                                              WalletRepository walletRepository,
                                                                                                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary,
                                                                                                              SubscriptionRepository subscriptionRepository) {
        return new PatchBulletIgnoreSubscriptionReservationStrategy(bulletRepository, walletRepository, findWalletDomainByIdBoundary, subscriptionRepository);
    }

    @Bean
    public FlagStrategyRegistry<PatchBulletInput, BulletOutput> patchBulletStrategyRegistry(
            DefaultPatchBulletStrategy defaultPatchBulletStrategy,
            PatchBulletIgnoreSubscriptionReservationStrategy patchBulletIgnoreSubscriptionReservationStrategy) {
        return FlagStrategyRegistry.<PatchBulletInput, BulletOutput>builder()
                .withDefault(defaultPatchBulletStrategy)
                .register(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, patchBulletIgnoreSubscriptionReservationStrategy)
                .build();
    }

    @Bean
    public FlagAwareExecutor<PatchBulletInput, BulletOutput> patchBulletExecutor(FlagManager flagManager,
                                                                                  FlagStrategyRegistry<PatchBulletInput, BulletOutput> patchBulletStrategyRegistry) {
        return new FlagAwareExecutor<>(flagManager, patchBulletStrategyRegistry);
    }

    @Bean
    public PatchBulletBoundary patchBulletBoundary(BulletRepository bulletRepository,
                                                   FlagAwareExecutor<PatchBulletInput, BulletOutput> patchBulletExecutor,
                                                   FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        PatchBulletUseCase useCase = new PatchBulletUseCase(bulletRepository, patchBulletExecutor, findWalletDomainByIdBoundary);
        return new TransactionalPatchBulletBoundary(useCase);
    }

    // -----------------------------------------------------------------------
    // Other bullet use cases
    // -----------------------------------------------------------------------

    @Bean
    public FindBulletByIdUseCase findBulletByIdUseCase(BulletRepository repository) {
        return new FindBulletByIdUseCase(repository);
    }

    @Bean
    public FindAllBulletsByIdsBoundary findAllBulletsByIdsUseCase(BulletRepository repository) {
        return new FindAllBulletsByIdsUseCase(repository);
    }

    @Bean
    public FindBulletsByWalletIdBoundary findBulletsByWalletIdBoundary(BulletRepository repository,
                                                                       FindWalletByIdBoundary findWalletByIdBoundary) {
        return new FindBulletsByWalletIdUseCase(repository, findWalletByIdBoundary);
    }

    @Bean
    public DeleteBulletByIdBoundary deleteBulletByIdBoundary(BulletRepository bulletRepository,
                                                             WalletRepository walletRepository,
                                                             PaymentRepository paymentRepository,
                                                             FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
        DeleteBulletByIdUseCase useCase = new DeleteBulletByIdUseCase(
                bulletRepository, walletRepository, paymentRepository, findWalletDomainByIdBoundary);
        return new TransactionalDeleteBulletByIdBoundary(useCase);
    }
}
