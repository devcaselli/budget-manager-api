package br.com.casellisoftware.budgetmanager.domain.bullet;

import br.com.casellisoftware.budgetmanager.domain.bullet.debit.DebitStrategy;
import br.com.casellisoftware.budgetmanager.domain.bullet.debit.StandardDebitStrategy;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a budget bullet.
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * New bullets should be obtained via {@link #create}; reconstruction from
 * persistence uses {@link #rebuild}.</p>
 */
public final class Bullet {

    private final String id;
    private final String description;
    private final Money budget;
    private final Money remaining;
    private final String walletId;

    public Bullet(String id, String description, Money budget, Money remaining, String walletId) {
        this.id = id;
        this.description = description;
        this.budget = budget;
        this.remaining = remaining;
        this.walletId = walletId;
    }

    public Bullet debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new Bullet(this.id, this.description, this.budget, newRemaining, this.walletId);
    }

    public Bullet pay(Payment payment) {
        return pay(payment, new StandardDebitStrategy());
    }

    public Bullet pay(Payment payment, DebitStrategy strategy) {
        Objects.requireNonNull(strategy, "strategy must not be null");
        Money newRemaining = strategy.applyDebit(this, payment.getAmount());
        return new Bullet(this.id, this.description, this.budget, newRemaining, this.walletId);
    }

    public Bullet patch(BulletPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        String patchedDescription = patch.description().orElse(this.description);
        Money patchedBudget = patch.budget().orElse(this.budget);
        Money patchedRemaining = patch.remaining().orElse(this.remaining);
        String patchedWalletId = patch.walletId().orElse(this.walletId);

        validatePatchedState(patchedDescription, patchedBudget, patchedRemaining, patchedWalletId);

        if (Objects.equals(this.description, patchedDescription)
                && Objects.equals(this.budget, patchedBudget)
                && Objects.equals(this.remaining, patchedRemaining)
                && Objects.equals(this.walletId, patchedWalletId)) {
            return this;
        }

        return new Bullet(this.id, patchedDescription, patchedBudget, patchedRemaining, patchedWalletId);
    }

    private void validatePatchedState(String description, Money budget, Money remaining, String walletId) {
        if (description == null) {
            throw new IllegalArgumentException("description must not be null");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        Objects.requireNonNull(budget, "budget must not be null");
        Objects.requireNonNull(remaining, "remaining must not be null");
        if (walletId == null) {
            throw new IllegalArgumentException("walletId must not be null");
        }
        if (walletId.isBlank()) {
            throw new IllegalArgumentException("walletId must not be blank");
        }
        if (!Objects.equals(this.walletId, walletId)) {
            throw new IllegalArgumentException("walletId is immutable");
        }
        if (remaining.isGreaterThan(budget)) {
            throw new IllegalArgumentException("remaining must not exceed budget");
        }
    }

    public static Bullet create(String description, Money budget, Money remaining, String walletId) {
        return new Bullet(UUID.randomUUID().toString(), description, budget, remaining, walletId);
    }

    public static Bullet rebuild(String id, String description, Money budget, Money remaining, String walletId) {
        return new Bullet(id, description, budget, remaining, walletId);
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

    public String getWalletId() {
        return walletId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bullet bullet && Objects.equals(id, bullet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
