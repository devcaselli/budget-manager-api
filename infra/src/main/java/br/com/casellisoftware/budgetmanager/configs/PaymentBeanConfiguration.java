package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.DeleteAllPaymentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindPaymentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindAllPaymentByExpenseIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeleteAllPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.DeletePaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindAllPaymentByExpenseIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindPaymentByIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.FindPaymentsByWalletIdUseCase;
import br.com.casellisoftware.budgetmanager.application.payment.usecase.PayExpenseUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.configs.transactional.TransactionalPayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentBeanConfiguration {

    @Bean
    public FindPaymentByIdUseCase findPaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new FindPaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public FindAllPaymentByExpenseIdBoundary findAllPaymentByExpenseIdUseCase(PaymentRepository paymentRepository) {
        return new FindAllPaymentByExpenseIdUseCase(paymentRepository);
    }

    @Bean
    public FindPaymentsByWalletIdBoundary findPaymentsByWalletIdBoundary(PaymentRepository paymentRepository,
                                                                         FindWalletByIdBoundary findWalletByIdBoundary) {
        return new FindPaymentsByWalletIdUseCase(paymentRepository, findWalletByIdBoundary);
    }

    @Bean
    public DeletePaymentByIdUseCase deletePaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new DeletePaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public DeleteAllPaymentByIdBoundary deleteAllPaymentByIdUseCase(PaymentRepository paymentRepository) {
        return new DeleteAllPaymentByIdUseCase(paymentRepository);
    }

    @Bean
    public PayExpenseBoundary payExpenseBoundary(PaymentRepository paymentRepository,
                                                 ExpenseRepository expenseRepository,
                                                 BulletRepository bulletRepository) {
        PayExpenseUseCase useCase = new PayExpenseUseCase(paymentRepository, expenseRepository, bulletRepository);
        return new TransactionalPayExpenseBoundary(useCase);
    }
}
