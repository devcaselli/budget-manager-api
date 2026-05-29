package br.com.casellisoftware.budgetmanager.domain.sync;

import java.util.List;

/**
 * Page of {@link PendingExpense} items returned by {@link IngestPendingSource}.
 */
public record PendingExpensePage(List<PendingExpense> items, long total) {

    public PendingExpensePage {
        items = items != null ? List.copyOf(items) : List.of();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
