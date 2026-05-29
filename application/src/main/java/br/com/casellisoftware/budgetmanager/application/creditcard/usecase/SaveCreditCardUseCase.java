package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.SaveCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class SaveCreditCardUseCase implements SaveCreditCardBoundary {

    private static final Logger log = LoggerFactory.getLogger(SaveCreditCardUseCase.class);

    private final CreditCardRepository creditCardRepository;

    public SaveCreditCardUseCase(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = Objects.requireNonNull(
                creditCardRepository, "creditCardRepository must not be null");
    }

    @Override
    public CreditCardOutput execute(CreditCardInput input) {
        Objects.requireNonNull(input, "input must not be null");
        log.info("Saving credit card name={}", input.name());

        CreditCard creditCard = CreditCard.create(input.name(), input.ownerId());
        CreditCard saved = creditCardRepository.save(creditCard);

        log.info("CreditCard saved id={}", saved.getId());
        return CreditCardOutputAssembler.from(saved);
    }
}
