package br.com.casellisoftware.budgetmanager.domain.installment;

/**
 * Sort order for installment wallet-context queries.
 *
 * <ul>
 *   <li>{@link #ENDING_SOON} – sorted by {@code lastInstallmentDate} ascending; installments
 *       that finish soonest appear first.</li>
 *   <li>{@link #ENDING_LATE} – sorted by {@code lastInstallmentDate} descending; installments
 *       that finish latest appear first.</li>
 * </ul>
 */
public enum InstallmentSortOrder {
    ENDING_SOON,
    ENDING_LATE
}
