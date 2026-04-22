package br.com.casellisoftware.budgetmanager.domain.bullet;

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
        return this.debit(payment.getAmount());
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
