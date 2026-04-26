package br.com.casellisoftware.budgetmanager.domain.bullet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Domain-native partial update object for {@link Bullet}.
 *
 * <p>It keeps the domain independent from application-layer DTOs while still
 * allowing explicit, typed patch semantics inside the aggregate.</p>
 */
public record BulletPatch(
        Optional<String> description,
        Optional<Money> budget,
        Optional<Money> remaining,
        Optional<String> walletId
) {

    public BulletPatch {
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(budget, "budget must not be null");
        Objects.requireNonNull(remaining, "remaining must not be null");
        Objects.requireNonNull(walletId, "walletId must not be null");
    }

    public static BulletPatch empty() {
        return new BulletPatch(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public BulletPatch withDescription(String description) {
        return description == null ? this : new BulletPatch(Optional.of(description), budget, remaining, walletId);
    }

    public BulletPatch withBudget(Money budget) {
        return budget == null ? this : new BulletPatch(description, Optional.of(budget), remaining, walletId);
    }

    public BulletPatch withRemaining(Money remaining) {
        return remaining == null ? this : new BulletPatch(description, budget, Optional.of(remaining), walletId);
    }

    public BulletPatch withWalletId(String walletId) {
        return walletId == null ? this : new BulletPatch(description, budget, remaining, Optional.of(walletId));
    }

    public boolean isEmpty() {
        return description.isEmpty() && budget.isEmpty() && remaining.isEmpty() && walletId.isEmpty();
    }

    public List<String> appliedFieldNames() {
        List<String> fields = new ArrayList<>();
        description.ifPresent(ignored -> fields.add("description"));
        budget.ifPresent(ignored -> fields.add("budget"));
        remaining.ifPresent(ignored -> fields.add("remaining"));
        walletId.ifPresent(ignored -> fields.add("walletId"));
        return List.copyOf(fields);
    }
}
