package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;

import java.util.Objects;

public class FindCreditCardByIdUseCase implements FindCreditCardByIdBoundary {

    private final CreditCardRepository creditCardRepository;

    public FindCreditCardByIdUseCase(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = Objects.requireNonNull(
                creditCardRepository, "creditCardRepository must not be null");
    }

    @Override
    public CreditCardOutput findById(String id, String ownerId) {
        return creditCardRepository.findById(id, ownerId)
                .map(CreditCardOutputAssembler::from)
                .orElseThrow(() -> new CreditCardNotFoundException(id));
    }
}
