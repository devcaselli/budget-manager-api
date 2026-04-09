package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Rich domain entity representing an expense.
 *
 * <p>An {@code Expense} is immutable: every state-changing operation returns a new
 * instance. Instances can only be obtained via {@link #create} (new expense) or
 * {@link #rehydrate} (reconstruction from persistence). Both factories enforce the
 * domain invariants.</p>
 *
 * <p>{@code remaining} represents the portion of {@code cost} that has not yet been
 * settled/paid-down for this expense. A newly created expense starts with
 * {@code remaining == cost}; as the expense is paid, {@code remaining} shrinks toward
 * zero via {@link #debit(Money)}. Invariants: {@code 0 <= remaining <= cost}.</p>
 */
public final class Expense {

    public static final int MAX_NAME_LENGTH = 120;

    private final String id;
    private final String walletId;
    private final String name;
    private final Money cost;
    private final Money remaining;
    private final LocalDate purchaseDate;

    private Expense(String id,
                    String walletId,
                    String name,
                    Money cost,
                    Money remaining,
                    LocalDate purchaseDate) {
        this.id = id;
        this.walletId = walletId;
        this.name = name;
        this.cost = cost;
        this.remaining = remaining;
        this.purchaseDate = purchaseDate;
    }

    /**
     * Creates a brand-new {@code Expense}. Generates a fresh id and initializes
     * {@code remaining} equal to {@code cost}. Rejects future {@code purchaseDate}.
     */
    public static Expense create(String walletId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate) {
        String id = UUID.randomUUID().toString();
        validateWalletId(walletId);
        validateName(name);
        validateCost(cost);
        validatePurchaseDateNotFuture(purchaseDate);
        return new Expense(id, walletId, name, cost, cost, purchaseDate);
    }

    /**
     * Reconstructs an {@code Expense} from persisted state. Enforces structural
     * invariants (non-null fields, positive cost, {@code 0 <= remaining <= cost})
     * but deliberately does <strong>not</strong> reject past/future
     * {@code purchaseDate}, since historical data may legitimately contain values
     * that {@link #create} would not accept (e.g., clock skew at write time).
     */
    public static Expense rehydrate(String id,
                                    String walletId,
                                    String name,
                                    Money cost,
                                    Money remaining,
                                    LocalDate purchaseDate) {
        validateId(id);
        validateWalletId(walletId);
        validateName(name);
        validateCost(cost);
        validateRemaining(remaining, cost);
        Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
        return new Expense(id, walletId, name, cost, remaining, purchaseDate);
    }

    /**
     * Returns a new {@code Expense} with {@code remaining} reduced by {@code amount}.
     * Fails if the resulting remaining would be negative.
     */
    public Expense debit(Money amount) {
        Objects.requireNonNull(amount, "amount must not be null");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("debit amount must be positive");
        }
        if (amount.isGreaterThan(this.remaining)) {
            throw new IllegalArgumentException(
                    "debit amount exceeds remaining: " + amount.amount() + " > " + this.remaining.amount());
        }
        Money newRemaining = this.remaining.subtract(amount);
        return new Expense(this.id, this.walletId, this.name, this.cost, newRemaining, this.purchaseDate);
    }

    private static void validateId(String id) {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    private static void validateWalletId(String walletId) {
        Objects.requireNonNull(walletId, "walletId must not be null");
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
    }

    private static void validateName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "name length must not exceed " + MAX_NAME_LENGTH);
        }
    }

    private static void validateCost(Money cost) {
        Objects.requireNonNull(cost, "cost must not be null");
        if (!cost.isPositive()) {
            throw new IllegalArgumentException("cost must be positive");
        }
    }

    private static void validateRemaining(Money remaining, Money cost) {
        Objects.requireNonNull(remaining, "remaining must not be null");
        if (remaining.isGreaterThan(cost)) {
            throw new IllegalArgumentException("remaining must not exceed cost");
        }
    }

    private static void validatePurchaseDateNotFuture(LocalDate purchaseDate) {
        Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("purchaseDate must not be in the future");
        }
    }

    public String getId() {
        return id;
    }

    public String getWalletId() {
        return walletId;
    }

    public String getName() {
        return name;
    }

    public Money getCost() {
        return cost;
    }

    public Money getRemaining() {
        return remaining;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Expense other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(walletId, other.walletId)
                && Objects.equals(name, other.name)
                && Objects.equals(cost, other.cost)
                && Objects.equals(remaining, other.remaining)
                && Objects.equals(purchaseDate, other.purchaseDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, walletId, name, cost, remaining, purchaseDate);
    }
}
