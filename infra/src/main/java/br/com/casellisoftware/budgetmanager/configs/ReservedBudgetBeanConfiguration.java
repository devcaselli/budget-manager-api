package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.DeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindActiveReservedBudgetsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindAllReservedBudgetsBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindReservedBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.LinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.SaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.UnlinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.DeleteReservedBudgetUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.FindActiveReservedBudgetsByMonthUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.FindAllReservedBudgetsUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.FindReservedBudgetByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.LinkReservedBudgetSourceUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.PatchReservedBudgetUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.ReservedBudgetConsumptionQuery;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.ReservedBudgetLinkValidationService;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.SaveReservedBudgetUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.UnlinkReservedBudgetSourceUseCase;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalLinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalUnlinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkCapValidator;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReservedBudgetBeanConfiguration {

    @Bean
    public ReservedBudgetLinkCapValidator reservedBudgetLinkCapValidator() {
        return new ReservedBudgetLinkCapValidator();
    }

    @Bean
    public ReservedBudgetLinkValidationService reservedBudgetLinkValidationService(
            SubscriptionRepository subscriptionRepository,
            InstallmentRepository installmentRepository,
            ShareRepository shareRepository,
            ReservedBudgetLinkCapValidator reservedBudgetLinkCapValidator) {
        return new ReservedBudgetLinkValidationService(
                subscriptionRepository, installmentRepository, shareRepository, reservedBudgetLinkCapValidator);
    }

    @Bean
    public ReservedBudgetConsumptionQuery reservedBudgetConsumptionQuery(
            SubscriptionRepository subscriptionRepository,
            InstallmentRepository installmentRepository,
            ShareRepository shareRepository) {
        return new ReservedBudgetConsumptionQuery(
                subscriptionRepository, installmentRepository, shareRepository);
    }

    @Bean
    public SaveReservedBudgetBoundary saveReservedBudgetBoundary(ReservedBudgetRepository reservedBudgetRepository,
                                                                 Clock clock) {
        SaveReservedBudgetUseCase useCase = new SaveReservedBudgetUseCase(reservedBudgetRepository, clock);
        return new TransactionalSaveReservedBudgetBoundary(useCase);
    }

    @Bean
    public PatchReservedBudgetBoundary patchReservedBudgetBoundary(ReservedBudgetRepository reservedBudgetRepository,
                                                                   Clock clock,
                                                                   ReservedBudgetLinkValidationService reservedBudgetLinkValidationService) {
        PatchReservedBudgetUseCase useCase = new PatchReservedBudgetUseCase(
                reservedBudgetRepository, clock, reservedBudgetLinkValidationService);
        return new TransactionalPatchReservedBudgetBoundary(useCase);
    }

    @Bean
    public DeleteReservedBudgetBoundary deleteReservedBudgetBoundary(ReservedBudgetRepository reservedBudgetRepository,
                                                                     Clock clock) {
        DeleteReservedBudgetUseCase useCase = new DeleteReservedBudgetUseCase(reservedBudgetRepository, clock);
        return new TransactionalDeleteReservedBudgetBoundary(useCase);
    }

    @Bean
    public LinkReservedBudgetSourceBoundary linkReservedBudgetSourceBoundary(
            ReservedBudgetRepository reservedBudgetRepository,
            SubscriptionRepository subscriptionRepository,
            InstallmentRepository installmentRepository,
            ReservedBudgetLinkValidationService reservedBudgetLinkValidationService,
            ReservedBudgetConsumptionQuery reservedBudgetConsumptionQuery,
            Clock clock) {
        LinkReservedBudgetSourceUseCase useCase = new LinkReservedBudgetSourceUseCase(
                reservedBudgetRepository, subscriptionRepository, installmentRepository,
                reservedBudgetLinkValidationService, reservedBudgetConsumptionQuery, clock);
        return new TransactionalLinkReservedBudgetSourceBoundary(useCase);
    }

    @Bean
    public UnlinkReservedBudgetSourceBoundary unlinkReservedBudgetSourceBoundary(
            ReservedBudgetRepository reservedBudgetRepository,
            ReservedBudgetConsumptionQuery reservedBudgetConsumptionQuery,
            Clock clock) {
        UnlinkReservedBudgetSourceUseCase useCase = new UnlinkReservedBudgetSourceUseCase(
                reservedBudgetRepository, reservedBudgetConsumptionQuery, clock);
        return new TransactionalUnlinkReservedBudgetSourceBoundary(useCase);
    }

    @Bean
    public FindReservedBudgetByIdBoundary findReservedBudgetByIdBoundary(
            ReservedBudgetRepository reservedBudgetRepository,
            ReservedBudgetConsumptionQuery reservedBudgetConsumptionQuery,
            Clock clock) {
        return new FindReservedBudgetByIdUseCase(reservedBudgetRepository, reservedBudgetConsumptionQuery, clock);
    }

    @Bean
    public FindAllReservedBudgetsBoundary findAllReservedBudgetsBoundary(ReservedBudgetRepository reservedBudgetRepository) {
        return new FindAllReservedBudgetsUseCase(reservedBudgetRepository);
    }

    @Bean
    public FindActiveReservedBudgetsByMonthBoundary findActiveReservedBudgetsByMonthBoundary(
            ReservedBudgetRepository reservedBudgetRepository,
            ReservedBudgetConsumptionQuery reservedBudgetConsumptionQuery) {
        return new FindActiveReservedBudgetsByMonthUseCase(reservedBudgetRepository, reservedBudgetConsumptionQuery);
    }
}
