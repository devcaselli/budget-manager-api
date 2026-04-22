package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record ExpensePatch(
        Optional<String> name,
        Optional<Money> cost,
        Optional<LocalDate> purchaseDate
) {

    public ExpensePatch {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(cost, "cost must not be null");
        Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
    }

    public static ExpensePatch empty() {
        return new ExpensePatch(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public ExpensePatch withName(String name) {
        return name == null ? this : new ExpensePatch(Optional.of(name), cost, purchaseDate);
    }

    public ExpensePatch withCost(Money cost) {
        return cost == null ? this : new ExpensePatch(name, Optional.of(cost), purchaseDate);
    }

    public ExpensePatch withPurchaseDate(LocalDate purchaseDate) {
        return purchaseDate == null ? this : new ExpensePatch(name, cost, Optional.of(purchaseDate));
    }

    public boolean isEmpty() {
        return name.isEmpty() && cost.isEmpty() && purchaseDate.isEmpty();
    }

    public List<String> appliedFieldNames() {
        List<String> fields = new ArrayList<>();
        name.ifPresent(ignored -> fields.add("name"));
        cost.ifPresent(ignored -> fields.add("cost"));
        purchaseDate.ifPresent(ignored -> fields.add("purchaseDate"));
        return List.copyOf(fields);
    }
}
