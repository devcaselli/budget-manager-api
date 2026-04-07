package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.mappers.ExpenseApplicationMapper;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BusinessLayerBeanConfiguration {

    @Bean
    public SaveExpenseUseCase  saveExpenseUseCase(ExpenseRepository repository, ExpenseApplicationMapper mapper) {
        return new SaveExpenseUseCase(mapper, repository);
    }
}
