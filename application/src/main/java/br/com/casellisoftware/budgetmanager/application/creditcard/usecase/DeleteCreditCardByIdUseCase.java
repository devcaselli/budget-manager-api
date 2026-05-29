package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.DeleteCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardInUseException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class DeleteCreditCardByIdUseCase implements DeleteCreditCardByIdBoundary {

    private static final Logger log = LoggerFactory.getLogger(DeleteCreditCardByIdUseCase.class);

    private final CreditCardRepository creditCardRepository;
    private final ExpenseRepository expenseRepository;
    private final InstallmentRepository installmentRepository;
    private final SubscriptionRepository subscriptionRepository;

    public DeleteCreditCardByIdUseCase(CreditCardRepository creditCardRepository,
                                       ExpenseRepository expenseRepository,
                                       InstallmentRepository installmentRepository,
                                       SubscriptionRepository subscriptionRepository) {
        this.creditCardRepository = Objects.requireNonNull(creditCardRepository);
        this.expenseRepository = Objects.requireNonNull(expenseRepository);
        this.installmentRepository = Objects.requireNonNull(installmentRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
    }

    @Override
    public void execute(String id, String ownerId) {
        Objects.requireNonNull(id, "id must not be null");

        if (!creditCardRepository.existsById(id, ownerId)) {
            throw new CreditCardNotFoundException(id);
        }

        List<String> expenseIds = expenseRepository.findIdsByCreditCardId(id, ownerId);
        List<String> installmentIds = installmentRepository.findIdsByCreditCardId(id, ownerId);
        boolean hasActiveSubscription = subscriptionRepository.existsActiveByCreditCardId(id, ownerId);
        if (!expenseIds.isEmpty() || !installmentIds.isEmpty() || hasActiveSubscription) {
            throw new CreditCardInUseException(id, expenseIds, installmentIds, hasActiveSubscription);
        }

        creditCardRepository.deleteById(id, ownerId);
        log.info("CreditCard deleted id={}", id);
    }
}
