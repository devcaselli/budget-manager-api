package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import java.time.YearMonth;

public interface FindCreditCardChargesBoundary {

    CreditCardChargesOutput execute(String creditCardId, YearMonth effectiveMonth, String ownerId);
}
