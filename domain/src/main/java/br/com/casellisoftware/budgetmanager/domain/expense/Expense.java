package br.com.casellisoftware.budgetmanager.domain.expense;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an expense.
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * New expenses should be obtained via {@link #create}; reconstruction
 * from persistence uses the public constructor directly (e.g. via MapStruct).</p>
 *
 * <p>{@code remaining} represents the portion of {@code cost} that has not yet been
 * settled. A newly created expense starts with {@code remaining == cost}; as the
 * expense is paid, {@code remaining} shrinks toward zero via {@link #debit(Money)}.</p>
 */
public final class Expense {

    public static final int MAX_NAME_LENGTH = 120;

    private final String id;
    private final String walletId;
    private final String name;
    private final Money cost;
    private final Money remaining;
    private final LocalDate purchaseDate;
    private final List<String> paymentIds;

    public Expense(String id,
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
        this.paymentIds = List.of();
    }

    public Expense(String id,
                   String walletId,
                   String name,
                   Money cost,
                   Money remaining,
                   LocalDate purchaseDate,
                   List<String> paymentIds) {
        this.id = id;
        this.walletId = walletId;
        this.name = name;
        this.cost = cost;
        this.remaining = remaining;
        this.purchaseDate = purchaseDate;
        this.paymentIds = paymentIds != null ? List.copyOf(paymentIds) : List.of();
    }

    /**
     * Creates a brand-new {@code Expense}. Generates a fresh id, initializes
     * {@code remaining} equal to {@code cost}, and enforces business rules:
     * cost must be positive and purchaseDate must not be in the future.
     */
    public static Expense create(String walletId,
                                 String name,
                                 Money cost,
                                 LocalDate purchaseDate) {
        if (!cost.isPositive()) {
            throw new IllegalArgumentException("cost must be positive");
        }
        if (purchaseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("purchaseDate must not be in the future");
        }
        return new Expense(UUID.randomUUID().toString(), walletId, name, cost, cost, purchaseDate);
    }

    /**
     * Returns a new {@code Expense} with {@code remaining} reduced by {@code amount}.
     */
    public Expense debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new Expense(this.id, this.walletId, this.name, this.cost, newRemaining, this.purchaseDate, this.paymentIds);
    }

    /**
     * Returns a new {@code Expense} with {@code payment.getId()} appended to
     * {@code paymentIds} and {@code remaining} debited by {@code payment.getAmount()}.
     */
    public Expense pay(Payment payment) {

        List<String> updatedIds = new ArrayList<>(this.paymentIds);
        updatedIds.add(payment.getId());
        return new Expense(this.id, this.walletId, this.name, this.cost, this.remaining, this.purchaseDate, updatedIds)
                .debit(payment.getAmount());
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

    public List<String> getPaymentIds() {
        return paymentIds;
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
