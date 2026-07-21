package br.com.casellisoftware.budgetmanager.application.pluggy.boundary;

import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;

import java.util.List;

public interface MaterializePluggyTransactionsBoundary {

    /**
     * Materializes the given transaction ids (or all previewed transactions when
     * {@code all} is {@code true}) into {@code Expense} records.
     *
     * @param ownerId        authenticated owner
     * @param itemId         Pluggy item id, scoped to the owner's connection
     * @param transactionIds explicit selection; ignored when {@code all} is {@code true}
     * @param all            when {@code true}, materializes every previewed transaction
     */
    SyncReport execute(String ownerId, String itemId, List<String> transactionIds, boolean all);
}
