package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

/**
 * Port for the link-source use case.
 */
public interface LinkReservedBudgetSourceBoundary {

    ReservedBudgetOutput execute(LinkReservedBudgetSourceInput input);
}
