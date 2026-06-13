package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.DeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindActiveReservedBudgetsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindAllReservedBudgetsBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindReservedBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.SaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.DeleteReservedBudgetUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.FindActiveReservedBudgetsByMonthUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.FindAllReservedBudgetsUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.FindReservedBudgetByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.PatchReservedBudgetUseCase;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase.SaveReservedBudgetUseCase;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ReservedBudgetBeanConfiguration {

    @Bean
    public SaveReservedBudgetBoundary saveReservedBudgetBoundary(ReservedBudgetRepository reservedBudgetRepository,
                                                                 Clock clock) {
        SaveReservedBudgetUseCase useCase = new SaveReservedBudgetUseCase(reservedBudgetRepository, clock);
        return new TransactionalSaveReservedBudgetBoundary(useCase);
    }

    @Bean
    public PatchReservedBudgetBoundary patchReservedBudgetBoundary(ReservedBudgetRepository reservedBudgetRepository,
                                                                   Clock clock) {
        PatchReservedBudgetUseCase useCase = new PatchReservedBudgetUseCase(reservedBudgetRepository, clock);
        return new TransactionalPatchReservedBudgetBoundary(useCase);
    }

    @Bean
    public DeleteReservedBudgetBoundary deleteReservedBudgetBoundary(ReservedBudgetRepository reservedBudgetRepository,
                                                                     Clock clock) {
        DeleteReservedBudgetUseCase useCase = new DeleteReservedBudgetUseCase(reservedBudgetRepository, clock);
        return new TransactionalDeleteReservedBudgetBoundary(useCase);
    }

    @Bean
    public FindReservedBudgetByIdBoundary findReservedBudgetByIdBoundary(ReservedBudgetRepository reservedBudgetRepository) {
        return new FindReservedBudgetByIdUseCase(reservedBudgetRepository);
    }

    @Bean
    public FindAllReservedBudgetsBoundary findAllReservedBudgetsBoundary(ReservedBudgetRepository reservedBudgetRepository) {
        return new FindAllReservedBudgetsUseCase(reservedBudgetRepository);
    }

    @Bean
    public FindActiveReservedBudgetsByMonthBoundary findActiveReservedBudgetsByMonthBoundary(ReservedBudgetRepository reservedBudgetRepository) {
        return new FindActiveReservedBudgetsByMonthUseCase(reservedBudgetRepository);
    }
}
