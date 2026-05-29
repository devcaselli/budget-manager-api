package br.com.casellisoftware.budgetmanager.domain.extrabudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an extra-budget allocation event.
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * New entities should be obtained via {@link #create}; reconstruction from
 * persistence uses {@link #rebuild}.</p>
 *
 * @implNote Time complexity for construction/validation: O(n), n = number of allocations.
 */
public final class ExtraBudget {

    private final String id;
    private final String ownerId;
    private final String description;
    private final String walletId;
    private final Money amount;
    private final List<ExtraBudgetAllocation> allocations;
    private final boolean deleted;
    private final LocalDateTime deletedAt;

    private ExtraBudget(String id, String ownerId, String description, String walletId, Money amount, List<ExtraBudgetAllocation> allocations, boolean deleted, LocalDateTime deletedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.description = requireNonBlank(description, "description");
        this.walletId = requireNonBlank(walletId, "walletId");
        this.amount = Objects.requireNonNull(amount, "amount must not be null");

        // Validate amount is positive
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("amount must be positive");
        }

        // Validate allocations not null and not empty
        Objects.requireNonNull(allocations, "allocations must not be null");
        if (allocations.isEmpty()) {
            throw new IllegalArgumentException("allocations must not be empty");
        }

        // Validate each allocation amount is positive
        for (ExtraBudgetAllocation allocation : allocations) {
            if (!allocation.amount().isPositive()) {
                throw new IllegalArgumentException("allocation amount must be positive for bulletId: " + allocation.bulletId());
            }
        }

        // Validate no duplicate bulletIds
        validateNoDuplicateBullets(allocations);

        // Validate sum of allocations equals amount
        validateAllocationsSum(amount, allocations);

        // Store allocations as unmodifiable copy
        this.allocations = List.copyOf(allocations);
        this.deleted = deleted;
        this.deletedAt = deletedAt;
    }

    /**
     * Factory method to create a new ExtraBudget with a generated UUID id.
     * Sets {@code deleted=false} and {@code deletedAt=null}.
     */
    public static ExtraBudget create(String ownerId, String description, String walletId, Money amount, List<ExtraBudgetAllocation> allocations) {
        return new ExtraBudget(UUID.randomUUID().toString(), ownerId, description, walletId, amount, allocations, false, null);
    }

    /**
     * Factory method to reconstruct an ExtraBudget from persistence.
     * Accepts all fields including id, deleted, and deletedAt.
     */
    public static ExtraBudget rebuild(String id, String ownerId, String description, String walletId, Money amount, List<ExtraBudgetAllocation> allocations, boolean deleted, LocalDateTime deletedAt) {
        return new ExtraBudget(id, ownerId, description, walletId, amount, allocations, deleted, deletedAt);
    }

    /**
     * Marks this ExtraBudget as deleted, returning a new instance.
     * If already deleted, returns {@code this} (idempotent).
     */
    public ExtraBudget markDeleted(LocalDateTime now) {
        Objects.requireNonNull(now, "now must not be null");
        if (this.deleted) {
            return this;
        }
        return new ExtraBudget(this.id, this.ownerId, this.description, this.walletId, this.amount, this.allocations, true, now);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getDescription() {
        return description;
    }

    public String getWalletId() {
        return walletId;
    }

    public Money getAmount() {
        return amount;
    }

    public List<ExtraBudgetAllocation> getAllocations() {
        return allocations;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExtraBudget that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(ownerId, that.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static void validateAllocationsSum(Money total, List<ExtraBudgetAllocation> allocations) {
        // allocations is guaranteed non-empty by the caller; orElseThrow is unreachable
        Money sum = allocations.stream()
            .map(ExtraBudgetAllocation::amount)
            .reduce(Money::add)
            .orElseThrow();
        if (!sum.equals(total)) {
            throw new IllegalArgumentException(
                "sum of allocations (" + sum.amount() + ") must equal amount (" + total.amount() + ")");
        }
    }

    private static void validateNoDuplicateBullets(List<ExtraBudgetAllocation> allocations) {
        long distinct = allocations.stream().map(ExtraBudgetAllocation::bulletId).distinct().count();
        if (distinct != allocations.size()) {
            throw new IllegalArgumentException("duplicate bulletId in allocations");
        }
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
