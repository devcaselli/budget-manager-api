package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import java.util.List;

/**
 * Input for partial update of a {@link br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard}.
 * All fields are optional; only non-null values are applied.
 */
public record PatchCreditCardInput(String id, String ownerId, List<String> labels) {
}
