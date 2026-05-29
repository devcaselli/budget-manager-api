package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.DeleteExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByBulletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.SaveExtraBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.usecase.DeleteExtraBudgetByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.extrabudget.usecase.FindExtraBudgetByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.extrabudget.usecase.FindExtraBudgetsByBulletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.extrabudget.usecase.FindExtraBudgetsByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.extrabudget.usecase.SaveExtraBudgetUseCase;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeleteExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSaveExtraBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExtraBudgetBeanConfiguration {

    @Bean
    public SaveExtraBudgetBoundary saveExtraBudgetBoundary(ExtraBudgetRepository extraBudgetRepository,
                                                           BulletRepository bulletRepository,
                                                           WalletRepository walletRepository) {
        SaveExtraBudgetUseCase useCase = new SaveExtraBudgetUseCase(
                extraBudgetRepository, bulletRepository, walletRepository);
        return new TransactionalSaveExtraBudgetBoundary(useCase);
    }

    @Bean
    public DeleteExtraBudgetByIdBoundary deleteExtraBudgetByIdBoundary(ExtraBudgetRepository extraBudgetRepository,
                                                                        BulletRepository bulletRepository) {
        DeleteExtraBudgetByIdUseCase useCase = new DeleteExtraBudgetByIdUseCase(
                extraBudgetRepository, bulletRepository);
        return new TransactionalDeleteExtraBudgetByIdBoundary(useCase);
    }

    @Bean
    public FindExtraBudgetByIdBoundary findExtraBudgetByIdBoundary(ExtraBudgetRepository extraBudgetRepository) {
        return new FindExtraBudgetByIdUseCase(extraBudgetRepository);
    }

    @Bean
    public FindExtraBudgetsByWalletIdBoundary findExtraBudgetsByWalletIdBoundary(ExtraBudgetRepository extraBudgetRepository) {
        return new FindExtraBudgetsByWalletIdUseCase(extraBudgetRepository);
    }

    @Bean
    public FindExtraBudgetsByBulletIdBoundary findExtraBudgetsByBulletIdBoundary(ExtraBudgetRepository extraBudgetRepository) {
        return new FindExtraBudgetsByBulletIdUseCase(extraBudgetRepository);
    }
}
