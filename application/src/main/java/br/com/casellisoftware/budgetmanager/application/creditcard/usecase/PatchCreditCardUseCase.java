package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardInput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class PatchCreditCardUseCase implements PatchCreditCardBoundary {

    private static final Logger log = LoggerFactory.getLogger(PatchCreditCardUseCase.class);

    private final CreditCardRepository creditCardRepository;

    public PatchCreditCardUseCase(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = Objects.requireNonNull(creditCardRepository, "creditCardRepository must not be null");
    }

    @Override
    public CreditCardOutput execute(PatchCreditCardInput input) {
        Objects.requireNonNull(input, "input must not be null");

        CreditCard existing = creditCardRepository.findById(input.id(), input.ownerId())
                .orElseThrow(() -> new CreditCardNotFoundException(input.id()));

        CreditCard updated = existing;
        if (input.labels() != null) {
            updated = existing.withLabels(input.labels());
        }

        if (updated == existing) {
            return CreditCardOutputAssembler.from(existing);
        }

        CreditCard saved = creditCardRepository.save(updated);
        log.info("CreditCard patched id={} labels={}", saved.getId(), saved.getLabels());
        return CreditCardOutputAssembler.from(saved);
    }
}
