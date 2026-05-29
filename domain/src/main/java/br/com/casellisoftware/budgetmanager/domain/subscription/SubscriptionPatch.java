package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Objects;
import java.util.Optional;

public record SubscriptionPatch(
        Optional<String> description,
        Optional<Money> newAmount,
        Optional<String> creditCardId,
        Optional<FlagEnum> flag
) {
    public SubscriptionPatch(Optional<String> description,
                             Optional<Money> newAmount) {
        this(description, newAmount, Optional.empty(), Optional.empty());
    }

    public SubscriptionPatch(Optional<String> description,
                             Optional<Money> newAmount,
                             Optional<FlagEnum> flag) {
        this(description, newAmount, Optional.empty(), flag);
    }

    public SubscriptionPatch {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(newAmount, "newAmount must not be null");
        Objects.requireNonNull(creditCardId, "creditCardId must not be null");
        Objects.requireNonNull(flag, "flag must not be null");
    }

    public static SubscriptionPatch empty() {
        return new SubscriptionPatch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public SubscriptionPatch withDescription(String description) {
        return description == null ? this : new SubscriptionPatch(Optional.of(description), newAmount, creditCardId, flag);
    }

    public SubscriptionPatch withNewAmount(Money newAmount) {
        return newAmount == null ? this : new SubscriptionPatch(description, Optional.of(newAmount), creditCardId, flag);
    }

    public SubscriptionPatch withCreditCardId(String creditCardId) {
        return creditCardId == null
                ? this
                : new SubscriptionPatch(description, newAmount, Optional.of(creditCardId), flag);
    }

    public SubscriptionPatch withFlag(FlagEnum flag) {
        return flag == null || flag == FlagEnum.NONE
                ? this
                : new SubscriptionPatch(description, newAmount, creditCardId, Optional.of(flag));
    }

    public boolean isEmpty() {
        return description.isEmpty() && newAmount.isEmpty() && creditCardId.isEmpty() && flag.isEmpty();
    }
}
