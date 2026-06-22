package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

/**
 * Identifies the type of item that can be linked to a {@link ReservedBudget}.
 *
 * <p>Kept separate from {@code ShareSourceType} to avoid coupling the sharing
 * and reserved-budget domains; a helper mapping is provided where needed.</p>
 */
public enum ReservedBudgetLinkSourceType {
    INSTALLMENT,
    SUBSCRIPTION
}
