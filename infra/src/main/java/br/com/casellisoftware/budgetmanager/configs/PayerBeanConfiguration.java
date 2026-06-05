package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.DeletePayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindAllPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindPayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindWalletPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PatchPayerBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.SavePayerBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.DeletePayerByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.EnsureTransientPayerUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.FindAllPayersUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.FindPayerByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.FindWalletPayersUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.PatchPayerUseCase;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.PayerAmountDueCalculator;
import br.com.casellisoftware.budgetmanager.application.payer.usecase.SavePayerUseCase;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalDeletePayerBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPatchPayerBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalSavePayerBoundary;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class PayerBeanConfiguration {

    @Bean
    public PayerAmountDueCalculator payerAmountDueCalculator(ShareRepository shareRepository,
                                                             br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository installmentRepository,
                                                             Clock clock) {
        return new PayerAmountDueCalculator(shareRepository, installmentRepository, clock);
    }

    @Bean
    public SavePayerBoundary savePayerBoundary(PayerRepository payerRepository,
                                               PayerAmountDueCalculator calculator) {
        return new TransactionalSavePayerBoundary(new SavePayerUseCase(payerRepository, calculator));
    }

    @Bean
    public PatchPayerBoundary patchPayerBoundary(PayerRepository payerRepository,
                                                 PayerAmountDueCalculator calculator,
                                                 ShareRepository shareRepository) {
        return new TransactionalPatchPayerBoundary(new PatchPayerUseCase(payerRepository, calculator, shareRepository));
    }

    @Bean
    public FindPayerByIdBoundary findPayerByIdBoundary(PayerRepository payerRepository,
                                                       PayerAmountDueCalculator calculator) {
        return new FindPayerByIdUseCase(payerRepository, calculator);
    }

    @Bean
    public FindAllPayersBoundary findAllPayersBoundary(PayerRepository payerRepository,
                                                       PayerAmountDueCalculator calculator) {
        return new FindAllPayersUseCase(payerRepository, calculator);
    }

    @Bean
    public FindWalletPayersBoundary findWalletPayersBoundary(PayerRepository payerRepository,
                                                             PayerAmountDueCalculator calculator) {
        return new FindWalletPayersUseCase(payerRepository, calculator);
    }

    @Bean
    public DeletePayerByIdBoundary deletePayerByIdBoundary(PayerRepository payerRepository,
                                                           ShareRepository shareRepository) {
        return new TransactionalDeletePayerBoundary(new DeletePayerByIdUseCase(payerRepository, shareRepository));
    }

    @Bean
    public EnsureTransientPayerUseCase ensureTransientPayerUseCase(PayerRepository payerRepository, Clock clock) {
        return new EnsureTransientPayerUseCase(payerRepository, clock);
    }
}
