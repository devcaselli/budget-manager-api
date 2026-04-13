package br.com.casellisoftware.budgetmanager.domain.bullet;

import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.util.Objects;
import java.util.UUID;

public class Bullet {

    private final  String id;
    private final String description;
    private final Money budget;
    private final Money remaining;
    private final String walletId;
    private final String paymentId;

    public Bullet(String id, String description, Money budget, Money remaining, String walletId) {
        this.id = id;
        this.description = description;
        this.budget = budget;
        this.remaining = remaining;
        this.walletId = walletId;
        this.paymentId = null;
    }

    public Bullet(String id, String description, Money budget, Money remaining, String walletId, String paymentId) {
        this.id = id;
        this.description = description;
        this.budget = budget;
        this.remaining = remaining;
        this.walletId = walletId;
        this.paymentId = paymentId;
    }


    public Bullet debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new Bullet(this.id, this.description, this.budget, newRemaining, this.walletId, this.paymentId);
    }

    public Bullet pay(Payment payment){
        Bullet paid = create(this.id, this.description, this.budget, this.remaining, this.walletId, payment.getId());
        return paid.debit(Money.of(payment.getAmount()));
    }


    public static Bullet create(String description, Money budget, Money remaining, String walletId){
        return new Bullet(UUID.randomUUID().toString(), description, budget, remaining, walletId);
    }

    public static Bullet create(String id, String description, Money budget, Money remaining, String walletId, String paymentId){
        return new Bullet(id, description, budget, remaining, walletId, paymentId);
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

    public String getPaymentId() {
        return paymentId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Bullet bullet = (Bullet) o;
        return Objects.equals(id, bullet.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
