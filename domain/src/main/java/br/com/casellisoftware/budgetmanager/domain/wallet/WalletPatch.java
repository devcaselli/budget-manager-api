package br.com.casellisoftware.budgetmanager.domain.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record WalletPatch(
        Optional<String> description,
        Optional<Money> budget,
        Optional<LocalDate> closedDate,
        Optional<Boolean> closed,
        Optional<WalletState> state,
        Optional<FlagEnum> flag
) {
    public WalletPatch(Optional<String> description,
                       Optional<Money> budget,
                       Optional<LocalDate> closedDate,
                       Optional<Boolean> closed,
                       Optional<WalletState> state) {
        this(description, budget, closedDate, closed, state, Optional.empty());
    }

    public WalletPatch {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(budget, "budget must not be null");
        Objects.requireNonNull(closedDate, "closedDate must not be null");
        Objects.requireNonNull(closed, "closed must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(flag, "flag must not be null");
    }

    public static WalletPatch empty() {
        return new WalletPatch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public WalletPatch withDescription(String description) {
        return description == null ? this : new WalletPatch(Optional.of(description), budget, closedDate, closed, state, flag);
    }

    public WalletPatch withBudget(Money budget) {
        return budget == null ? this : new WalletPatch(description, Optional.of(budget), closedDate, closed, state, flag);
    }

    public WalletPatch withClosedDate(LocalDate closedDate) {
        return closedDate == null ? this : new WalletPatch(description, budget, Optional.of(closedDate), closed, state, flag);
    }

    public WalletPatch withClosed(Boolean closed) {
        return closed == null ? this : new WalletPatch(description, budget, closedDate, Optional.of(closed), state, flag);
    }

    public WalletPatch withState(WalletState state) {
        return state == null ? this : new WalletPatch(description, budget, closedDate, closed, Optional.of(state), flag);
    }

    public WalletPatch withFlag(FlagEnum flag) {
        return flag == null || flag == FlagEnum.NONE
                ? this
                : new WalletPatch(description, budget, closedDate, closed, state, Optional.of(flag));
    }

    public boolean isEmpty() {
        return description.isEmpty() && budget.isEmpty() && closedDate.isEmpty() && closed.isEmpty() && state.isEmpty() && flag.isEmpty();
    }

    public List<String> appliedFieldNames() {
        List<String> fields = new ArrayList<>();
        description.ifPresent(ignored -> fields.add("description"));
        budget.ifPresent(ignored -> fields.add("budget"));
        closedDate.ifPresent(ignored -> fields.add("closedDate"));
        closed.ifPresent(ignored -> fields.add("closed"));
        state.ifPresent(ignored -> fields.add("state"));
        flag.ifPresent(ignored -> fields.add("flag"));
        return List.copyOf(fields);
    }
}
