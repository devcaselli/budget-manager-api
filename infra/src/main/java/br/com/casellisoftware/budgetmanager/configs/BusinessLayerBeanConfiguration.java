package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.expense.usecase.FindExpensesByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.SaveWalletUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessLayerBeanConfiguration {

    @Bean
    public SaveExpenseUseCase saveExpenseUseCase(ExpenseRepository repository, FindWalletByIdUseCase findWalletByIdUseCase) {
        return new SaveExpenseUseCase(repository, findWalletByIdUseCase);
    }

    @Bean
    public FindExpensesByWalletIdUseCase findExpensesByWalletIdUseCase(ExpenseRepository repository, FindWalletByIdUseCase findWalletByIdUseCase) {
        return new FindExpensesByWalletIdUseCase(repository, findWalletByIdUseCase);
    }

    @Bean
    public SaveWalletUseCase saveWalletBoundary(WalletRepository walletRepository) {
        return new SaveWalletUseCase(walletRepository);
    }

    @Bean
    public FindWalletByIdUseCase  findWalletByIdUseCase(WalletRepository walletRepository) {
        return new FindWalletByIdUseCase(walletRepository);
    }
}
