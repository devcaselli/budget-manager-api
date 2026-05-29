package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindAllCreditCardsBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

import java.util.List;
import java.util.Objects;

public class FindAllCreditCardsUseCase implements FindAllCreditCardsBoundary {

    private final CreditCardRepository creditCardRepository;

    public FindAllCreditCardsUseCase(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = Objects.requireNonNull(
                creditCardRepository, "creditCardRepository must not be null");
    }

    @Override
    public PageResult<CreditCardOutput> execute(int page, int size, String ownerId) {
        PageResult<CreditCard> result = creditCardRepository.findAll(page, size, ownerId);
        List<CreditCardOutput> content = result.content().stream()
                .map(CreditCardOutputAssembler::from)
                .toList();
        return new PageResult<>(content, result.page(), result.size(),
                result.totalElements(), result.totalPages());
    }
}
