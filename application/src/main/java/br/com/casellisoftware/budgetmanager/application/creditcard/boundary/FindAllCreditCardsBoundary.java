package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;

public interface FindAllCreditCardsBoundary {
    PageResult<CreditCardOutput> execute(int page, int size, String ownerId);
}
