package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.DeleteCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardChargesBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindAllCreditCardsBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.SaveCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.DeleteCreditCardByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.FindCreditCardChargesUseCase;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.FindCreditCardExpensesUseCase;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.FindAllCreditCardsUseCase;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.FindCreditCardByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.PatchCreditCardUseCase;
import br.com.casellisoftware.budgetmanager.application.creditcard.usecase.SaveCreditCardUseCase;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CreditCardBeanConfiguration {

    @Bean
    public SaveCreditCardBoundary saveCreditCardBoundary(CreditCardRepository repo) {
        return new SaveCreditCardUseCase(repo);
    }

    @Bean
    public FindCreditCardByIdBoundary findCreditCardByIdBoundary(CreditCardRepository repo) {
        return new FindCreditCardByIdUseCase(repo);
    }

    @Bean
    public FindAllCreditCardsBoundary findAllCreditCardsBoundary(CreditCardRepository repo) {
        return new FindAllCreditCardsUseCase(repo);
    }

    @Bean
    public FindCreditCardExpensesBoundary findCreditCardExpensesBoundary(CreditCardRepository creditCardRepository,
                                                                         ExpenseRepository expenseRepository,
                                                                         WalletRepository walletRepository) {
        return new FindCreditCardExpensesUseCase(
                creditCardRepository,
                expenseRepository,
                walletRepository
        );
    }

    @Bean
    public FindCreditCardChargesBoundary findCreditCardChargesBoundary(CreditCardRepository creditCardRepository,
                                                                       ExpenseRepository expenseRepository,
                                                                       WalletRepository walletRepository,
                                                                       InstallmentRepository installmentRepository,
                                                                       SubscriptionRepository subscriptionRepository,
                                                                       ShareRepository shareRepository) {
        return new FindCreditCardChargesUseCase(
                creditCardRepository,
                expenseRepository,
                walletRepository,
                installmentRepository,
                subscriptionRepository,
                shareRepository
        );
    }

    @Bean
    public PatchCreditCardBoundary patchCreditCardBoundary(CreditCardRepository creditCardRepository) {
        return new PatchCreditCardUseCase(creditCardRepository);
    }

    @Bean
    public DeleteCreditCardByIdBoundary deleteCreditCardByIdBoundary(CreditCardRepository creditCardRepository,
                                                                     ExpenseRepository expenseRepository,
                                                                     InstallmentRepository installmentRepository,
                                                                     SubscriptionRepository subscriptionRepository) {
        return new DeleteCreditCardByIdUseCase(
                creditCardRepository,
                expenseRepository,
                installmentRepository,
                subscriptionRepository
        );
    }
}
