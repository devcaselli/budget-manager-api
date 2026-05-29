package br.com.casellisoftware.budgetmanager.domain.payment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PaymentPatch(
        Optional<Money> amount,
        Optional<String> details,
        Optional<FlagEnum> flag
) {
    public PaymentPatch(Optional<Money> amount,
                        Optional<String> details) {
        this(amount, details, Optional.empty());
    }

    public PaymentPatch {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(details, "details must not be null");
        Objects.requireNonNull(flag, "flag must not be null");
    }

    public static PaymentPatch empty() {
        return new PaymentPatch(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public PaymentPatch withAmount(Money amount) {
        return amount == null ? this : new PaymentPatch(Optional.of(amount), details, flag);
    }

    public PaymentPatch withDetails(String details) {
        return details == null ? this : new PaymentPatch(amount, Optional.of(details), flag);
    }

    public PaymentPatch withFlag(FlagEnum flag) {
        return flag == null || flag == FlagEnum.NONE
                ? this
                : new PaymentPatch(amount, details, Optional.of(flag));
    }

    public boolean isEmpty() {
        return amount.isEmpty() && details.isEmpty() && flag.isEmpty();
    }

    public List<String> appliedFieldNames() {
        List<String> fields = new ArrayList<>();
        amount.ifPresent(ignored -> fields.add("amount"));
        details.ifPresent(ignored -> fields.add("details"));
        flag.ifPresent(ignored -> fields.add("flag"));
        return List.copyOf(fields);
    }
}
