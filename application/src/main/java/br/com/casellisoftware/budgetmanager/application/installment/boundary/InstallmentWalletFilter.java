package br.com.casellisoftware.budgetmanager.application.installment.boundary;

import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentSortOrder;

/**
 * Query parameters for the wallet-context installment search.
 *
 * @param page           zero-based page index
 * @param size           page size (≥ 1)
 * @param creditCardId   optional credit-card filter; {@code null} means no filter
 * @param sortOrder      sort by {@code lastInstallmentDate}; defaults to {@link InstallmentSortOrder#ENDING_SOON}
 *                       when {@code null}
 */
public record InstallmentWalletFilter(
        int page,
        int size,
        String creditCardId,
        InstallmentSortOrder sortOrder
) {
    public InstallmentWalletFilter {
        if (page < 0) throw new IllegalArgumentException("page must not be negative");
        if (size < 1) throw new IllegalArgumentException("size must be at least 1");
        if (sortOrder == null) sortOrder = InstallmentSortOrder.ENDING_SOON;
    }
}
