package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.SaveWalletUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessLayerBeanConfiguration {

    @Bean
    public SaveExpenseUseCase saveExpenseUseCase(ExpenseRepository repository) {
        return new SaveExpenseUseCase(repository);
    }

    @Bean
    public SaveWalletUseCase saveWalletBoundary(WalletRepository walletRepository) {
        return new SaveWalletUseCase(walletRepository);
    }
}
