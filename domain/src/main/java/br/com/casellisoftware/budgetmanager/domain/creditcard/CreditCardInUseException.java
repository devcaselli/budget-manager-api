package br.com.casellisoftware.budgetmanager.domain.creditcard;

import java.util.List;
import java.util.Objects;

/**
 * Thrown when deleting a CreditCard that is still referenced by at least one
 * Expense, Installment, or active Subscription.
 */
public class CreditCardInUseException extends RuntimeException {

    private final List<String> expenseIds;
    private final List<String> installmentIds;
    private final boolean hasActiveSubscription;

    public CreditCardInUseException(String creditCardId,
                                    List<String> expenseIds,
                                    List<String> installmentIds) {
        this(creditCardId, expenseIds, installmentIds, false);
    }

    public CreditCardInUseException(String creditCardId,
                                    List<String> expenseIds,
                                    List<String> installmentIds,
                                    boolean hasActiveSubscription) {
        super(buildMessage(creditCardId, expenseIds, installmentIds, hasActiveSubscription));
        this.expenseIds = List.copyOf(Objects.requireNonNullElse(expenseIds, List.of()));
        this.installmentIds = List.copyOf(Objects.requireNonNullElse(installmentIds, List.of()));
        this.hasActiveSubscription = hasActiveSubscription;
    }

    private static String buildMessage(String creditCardId,
                                       List<String> expenseIds,
                                       List<String> installmentIds,
                                       boolean hasActiveSubscription) {
        List<String> normalizedExpenses = Objects.requireNonNullElse(expenseIds, List.of());
        List<String> normalizedInstallments = Objects.requireNonNullElse(installmentIds, List.of());
        String base = "CreditCard " + creditCardId + " is referenced by "
                + normalizedExpenses.size() + " expense(s) and "
                + normalizedInstallments.size() + " installment(s)";
        if (hasActiveSubscription) {
            base = base + " and at least one active subscription";
        }
        return base + " and cannot be deleted";
    }

    public List<String> getExpenseIds() {
        return expenseIds;
    }

    public List<String> getInstallmentIds() {
        return installmentIds;
    }

    public int getExpenseCount() {
        return expenseIds.size();
    }

    public int getInstallmentCount() {
        return installmentIds.size();
    }

    public boolean hasActiveSubscription() {
        return hasActiveSubscription;
    }
}
