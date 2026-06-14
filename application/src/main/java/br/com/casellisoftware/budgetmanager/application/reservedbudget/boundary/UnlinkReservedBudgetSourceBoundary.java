package br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary;

/**
 * Port for the unlink-source use case.
 */
public interface UnlinkReservedBudgetSourceBoundary {

    ReservedBudgetOutput execute(UnlinkReservedBudgetSourceInput input);
}
