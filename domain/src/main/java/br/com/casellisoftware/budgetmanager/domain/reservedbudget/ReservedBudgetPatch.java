package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Objects;
import java.util.Optional;

/**
 * Optional-based patch for {@link ReservedBudget}. Absent fields are left unchanged.
 * A present {@code newAmount} is materialized as a new version effective from the current
 * month onward (mirrors {@code SubscriptionPatch}).
 */
public record ReservedBudgetPatch(
        Optional<String> description,
        Optional<String> details,
        Optional<Money> newAmount,
        Optional<FlagEnum> flag
) {
    public ReservedBudgetPatch {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(details, "details must not be null");
        Objects.requireNonNull(newAmount, "newAmount must not be null");
        Objects.requireNonNull(flag, "flag must not be null");
    }

    public static ReservedBudgetPatch empty() {
        return new ReservedBudgetPatch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public ReservedBudgetPatch withDescription(String description) {
        return description == null ? this : new ReservedBudgetPatch(Optional.of(description), details, newAmount, flag);
    }

    public ReservedBudgetPatch withDetails(String details) {
        return details == null ? this : new ReservedBudgetPatch(description, Optional.of(details), newAmount, flag);
    }

    public ReservedBudgetPatch withNewAmount(Money newAmount) {
        return newAmount == null ? this : new ReservedBudgetPatch(description, details, Optional.of(newAmount), flag);
    }

    public ReservedBudgetPatch withFlag(FlagEnum flag) {
        return flag == null || flag == FlagEnum.NONE
                ? this
                : new ReservedBudgetPatch(description, details, newAmount, Optional.of(flag));
    }

    public boolean isEmpty() {
        return description.isEmpty() && details.isEmpty() && newAmount.isEmpty() && flag.isEmpty();
    }
}
