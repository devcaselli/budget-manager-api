package br.com.casellisoftware.budgetmanager.domain.wallet;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a wallet (budget envelope).
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * Reconstruction from persistence uses the public constructor directly
 * (e.g. via MapStruct).</p>
 *
 * <p>{@code remaining} represents the portion of {@code budget} that has not
 * yet been consumed by expenses. A newly created wallet starts with
 * {@code remaining == budget}; as expenses are debited, {@code remaining}
 * shrinks toward zero via {@link #debit(Money)}.</p>
 */
public final class Wallet {

    private final String id;
    private final String description;
    private final Money budget;
    private final Money remaining;
    private final LocalDate startDate;
    private final LocalDate closedDate;
    private final Boolean closed;

    public Wallet(String id,
                  String description,
                  Money budget,
                  Money remaining, LocalDate startDate,
                  LocalDate closedDate,
                  Boolean closed) {
        this.id = id;
        this.description = description;
        this.budget = budget;
        this.remaining = remaining;
        this.startDate = startDate;
        this.closedDate = closedDate;
        this.closed = closed;
    }

    public static Wallet create(String description,
                                Money budget,
                                LocalDate closedDate,
                                LocalDate startDate,
                                Boolean closed) {
        return new Wallet(UUID.randomUUID().toString(), description, budget, budget, startDate, closedDate, closed);
    }

    /**
     * Returns a new {@code Wallet} with {@code remaining} reduced by {@code amount}.
     */
    public Wallet debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new Wallet(this.id, this.description, this.budget, newRemaining, this.startDate, this.closedDate, this.closed);
    }

    /**
     * Applies an explicit partial update. Financial state derived from debits
     * ({@code remaining}) and lifecycle identity ({@code startDate}) are not patchable.
     */
    public Wallet patch(WalletPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        String patchedDescription = patch.description().orElse(this.description);
        Money patchedBudget = patch.budget().orElse(this.budget);
        LocalDate patchedClosedDate = patch.closedDate().orElse(this.closedDate);
        Boolean patchedClosed = patch.closed().orElse(this.closed);

        validatePatchedState(patchedBudget);

        if (Objects.equals(this.description, patchedDescription)
                && Objects.equals(this.budget, patchedBudget)
                && Objects.equals(this.closedDate, patchedClosedDate)
                && Objects.equals(this.closed, patchedClosed)) {
            return this;
        }

        return new Wallet(this.id, patchedDescription, patchedBudget, this.remaining, this.startDate, patchedClosedDate, patchedClosed);
    }

    private void validatePatchedState(Money budget) {
        Objects.requireNonNull(budget, "budget must not be null");
        if (this.remaining != null && this.remaining.isGreaterThan(budget)) {
            throw new IllegalArgumentException("remaining must not exceed budget");
        }
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Money getBudget() {
        return budget;
    }

    public Money getRemaining() {
        return remaining;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getClosedDate() {
        return closedDate;
    }

    public Boolean getClosed() {
        return closed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(description, other.description)
                && Objects.equals(budget, other.budget)
                && Objects.equals(remaining, other.remaining)
                && Objects.equals(startDate, other.startDate)
                && Objects.equals(closedDate, other.closedDate)
                && Objects.equals(closed, other.closed);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, budget, remaining, startDate, closedDate, closed);
    }
}
