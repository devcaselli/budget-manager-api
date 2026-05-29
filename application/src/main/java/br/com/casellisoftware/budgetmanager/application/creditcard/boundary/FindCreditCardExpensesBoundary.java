package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

public interface FindCreditCardExpensesBoundary {

    CreditCardExpensesOutput execute(String creditCardId, FindCreditCardExpensesInput input, String ownerId);
}
